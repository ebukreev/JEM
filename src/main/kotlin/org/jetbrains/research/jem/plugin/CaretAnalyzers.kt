package org.jetbrains.research.jem.plugin

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCallExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.thomas.checkMate.discovery.general.Discovery
import com.thomas.checkMate.editing.MultipleMethodException
import com.thomas.checkMate.editing.PsiMethodCallExpressionExtractor
import com.thomas.checkMate.editing.PsiStatementExtractor
import javassist.bytecode.Descriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor
import org.jetbrains.research.jem.interaction.InfoReader
import org.jetbrains.research.jem.interaction.JarAnalyzer
import java.io.File

interface CaretAnalyzer {

    val hintManager: HintManager
        get() = HintManager.getInstance()

    fun analyze(psiFile: PsiFile, caret: Caret)
            : Map<PsiType, Set<Discovery>>?

    fun String.toJsonPath(): String =
            ".${File.pathSeparator}analyzedLibs/${this
                    .substringAfterLast(File.pathSeparator)
                    .removeSuffix(".jar")}.json"

}

object JavaCaretAnalyzer: CaretAnalyzer {
    override fun analyze(psiFile: PsiFile, caret: Caret): Map<PsiType, Set<Discovery>>? {
        val statementExtractor = PsiStatementExtractor(psiFile, caret.selectionStart, caret.selectionEnd)
        val methodCallExpressionExtractor = PsiMethodCallExpressionExtractor(statementExtractor)
        val editor = caret.editor
        val psiMethodCalls: Set<PsiCallExpression>
        psiMethodCalls = try {
            methodCallExpressionExtractor.extract()
        } catch (mme: MultipleMethodException) {
            hintManager.showErrorHint(editor, "Please keep your selection within one method")
            return null
        }
        if (psiMethodCalls.isEmpty()) {
            hintManager.showErrorHint(editor, "No expressions found in current selection")
            return null
        }
        return getDiscoveredExceptionsMap(psiMethodCalls, editor.project ?: return null)
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

    private fun PsiMethod.getJarPath() =
            this.containingFile.virtualFile.toString()
            .replaceAfterLast(".jar", "")
            .replaceBefore("://", "")
            .removePrefix("://")
}

object KotlinCaretAnalyzer: CaretAnalyzer {
    override fun analyze(psiFile: PsiFile, caret: Caret): Map<PsiType, Set<Discovery>>? {
        val kStatementExtractor  = KtExpressionExtractor(
                psiFile as KtFile,
                caret.selectionStart, caret.selectionEnd)
        val kCallExtractor = KPsiMethodCallExpressionExtractor(kStatementExtractor)
        val editor = caret.editor
        val psiMethodCalls: Set<KtCallElement>
        psiMethodCalls = try {
            kCallExtractor.extract()
        } catch (mme: MultipleMethodException) {
            hintManager.showErrorHint(editor, "Please keep your selection within one method")
            return null
        }
        if (psiMethodCalls.isEmpty()) {
            hintManager.showErrorHint(editor, "No expressions found in current selection")
            return null
        }
        return getDiscoveredExceptionsMap(psiMethodCalls, editor.project ?: return null)
    }

    private fun CallableDescriptor.getJarPath(): String =
            this.findPsi()!!.containingFile.virtualFile.toString()
                .replaceAfterLast(".jar", "")
                .replaceBefore("://", "")
                .removePrefix("://")

    private fun getDiscoveredExceptionsMap(psiMethodCalls: Set<KtCallElement>, project: Project): Map<PsiType, Set<Discovery>> {
        val result = mutableMapOf<PsiType, MutableSet<Discovery>>()
        for (call in psiMethodCalls) {
            val method = call.getResolvedCall(call.analyze())?.resultingDescriptor ?: continue
            if (!method.findPsi()?.containingFile?.virtualFile.toString().startsWith("jar")) {
                continue
            }
            val jarPath = method.getJarPath()
            val jsonPath = jarPath.toJsonPath()
            if (!File(jsonPath).exists()) {
                JarAnalyzer.analyze(jarPath)
            }
            val lib = InfoReader.read(jsonPath)
            val name = method.name
            val `class` = method.containingDeclaration.fqNameSafe.toString()
            val descriptor = descriptorFor(method)
            val exceptions = lib.classes
                    .find { it.className == `class` }
                    ?.methods
                    ?.find { it.methodName == name.toString() && it.descriptor == descriptor }
                    ?.exceptions ?: emptySet()
            exceptions.forEach {
                val typeOfIt = PsiType.getTypeByName(it, project, call.resolveScope)
                val discovery = Discovery(typeOfIt, call, method.findPsi() as PsiMethod)
                result.getOrPut(typeOfIt) { mutableSetOf() }.add(discovery)
            }
        }
        return result
    }

    private fun descriptorFor(method: CallableDescriptor): String =
            buildString {
                append("(")
                method.valueParameters.forEach {
                    append(Descriptor.of(it.source.getPsi()?.text?.replace(" classname", "")))
                }
                append(")")
                append(Descriptor.of(method.returnType.toClassDescriptor?.fqNameSafe.toString()))
            }
}