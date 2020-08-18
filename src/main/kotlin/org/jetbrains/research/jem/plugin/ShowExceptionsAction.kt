package org.jetbrains.research.jem.plugin

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.thomas.checkMate.discovery.general.Discovery
import com.thomas.checkMate.editing.MultipleMethodException
import com.thomas.checkMate.editing.PsiMethodCallExpressionExtractor
import com.thomas.checkMate.editing.PsiStatementExtractor
import javassist.bytecode.Descriptor
import org.jetbrains.research.jem.interaction.InfoReader
import org.jetbrains.research.jem.interaction.JarAnalyzer
import java.io.File

class ShowExceptionsAction : AnAction() {

    private val hintManager: HintManager = HintManager.getInstance()

    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)
        val project = e.project
        e.place
        if (psiFile == null || editor == null) {
            return
        }
        val currentCaret = editor.caretModel.currentCaret
        val statementExtractor = PsiStatementExtractor(psiFile, currentCaret.selectionStart, currentCaret.selectionEnd)
        val methodCallExpressionExtractor = PsiMethodCallExpressionExtractor(statementExtractor)
        val psiMethodCalls: Set<PsiCallExpression>
        psiMethodCalls = try {
            methodCallExpressionExtractor.extract()
        } catch (mme: MultipleMethodException) {
            hintManager.showErrorHint(editor, "Please keep your selection within one method")
            return
        }
        if (psiMethodCalls.isEmpty()) {
            hintManager.showErrorHint(editor, "No expressions found in current selection")
            return
        }

        val discoveredExceptions = getDiscoveredExceptionsMap(psiMethodCalls, project!!)

        val generateDialog = GenerateDialog(discoveredExceptions, psiFile)
        generateDialog.show()
        if (generateDialog.isOK) {
             currentCaret.removeSelection()
        }
        psiFile.navigate(true)
    }

    private fun getDiscoveredExceptionsMap(psiMethodCalls: Set<PsiCallExpression>, project: Project)
            : Map<PsiType, Set<Discovery>> {
        val result = mutableMapOf<PsiType, MutableSet<Discovery>>()
        for (call in psiMethodCalls) {
            val method = call.resolveMethod() ?: continue
            if (!method.containingFile.virtualFile.toString().startsWith("jar")) {
                continue
            }
            val jarPath = method.getJarPath()
            val jsonPath = jarPath.toJsonPath()
            if (!File(jsonPath).exists()) {
                JarAnalyzer.analyze(jarPath)
            }
            val lib = InfoReader.read(jsonPath)
            val name = method.name
            val `class` = method.containingClass?.qualifiedName.toString()
            val descriptor = descriptorFor(method)
            val exceptions = lib.classes
                .find { it.className == `class` }
                ?.methods
                ?.find { it.methodName == name && it.descriptor == descriptor }
                ?.exceptions ?: emptySet()
            exceptions.forEach {
                val typeOfIt = PsiType.getTypeByName(it, project, call.resolveScope)
                val discovery = Discovery(typeOfIt, call, method)
                result.getOrPut(typeOfIt) { mutableSetOf() }.add(discovery)
            }
        }
        return result
    }

    private fun descriptorFor(method: PsiMethod): String =
            buildString {
                append("(")
                method.parameterList.parameters.forEach {
                    append(Descriptor.of(it.type.canonicalText))
                }
                append(")")
                append(Descriptor.of(method.returnType?.canonicalText))
            }
}

private fun PsiMethod.getJarPath(): String =
        this.containingFile.virtualFile.toString()
                .replaceAfterLast(".jar", "")
                .replaceBefore("://", "")
                .removePrefix("://")

private fun String.toJsonPath(): String =
        "./analyzedLibs/${this
                .substringAfterLast("/")
                .removeSuffix(".jar")}.json"
