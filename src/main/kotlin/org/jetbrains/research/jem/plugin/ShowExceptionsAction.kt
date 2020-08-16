package org.jetbrains.research.jem.plugin

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiCallExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiStatement
import com.intellij.psi.util.PsiTreeUtil
import com.thomas.checkMate.editing.MultipleMethodException
import com.thomas.checkMate.editing.PsiMethodCallExpressionExtractor
import com.thomas.checkMate.editing.PsiStatementExtractor
import javassist.bytecode.Descriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.research.jem.interaction.InfoReader
import org.jetbrains.research.jem.interaction.JarAnalyzer
import java.io.File

class ShowExceptionsAction : AnAction() {

    private val hintManager: HintManager = HintManager.getInstance()

    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (psiFile == null || editor == null) {
            return
        }
        val currentCaret = editor.caretModel.currentCaret
//        val methodCall: PsiCallExpression =
//            findCallExpression(currentCaret, psiFile, editor) ?: return
        val statementExtractor = PsiStatementExtractor(psiFile,
                        currentCaret.selectionStart,
                        currentCaret.selectionEnd)
        val methodCallExpressionExtractor =
               PsiMethodCallExpressionExtractor(statementExtractor)
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
        val methodCall = psiMethodCalls.first()
        val method = methodCall.resolveMethod() ?: return
        if (!method.containingFile.virtualFile.toString().startsWith("jar")) {
            hintManager.showErrorHint(editor, "This is not a method call from a dependency")
            return
        }
        val jar = method.containingFile.virtualFile.toString()
                .replaceAfterLast(".jar", "")
                .replaceBefore("://", "")
                .removePrefix("://")
        val jsonPath = "./analyzedLibs/${jar
                .substringAfterLast("/")
                .removeSuffix(".jar")}.json"
        if (!File(jsonPath).exists()) {
            JarAnalyzer.analyze(jar)
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
        val callToExceptions = methodCall to exceptions
        val generateDialog = GenerateDialog(psiFile, callToExceptions, name, `class`)
        generateDialog.show()
        currentCaret.removeSelection()
        psiFile.navigate(true)
    }

    private fun findCallExpression(caret: Caret, psiFile: PsiFile, editor: Editor)
            : PsiCallExpression? {
        val selectedStatements = mutableListOf<PsiStatement>()
        for (i in caret.selectionStart..caret.selectionEnd) {
            val psiElement = psiFile.findElementAt(i)
            if (psiElement != null && psiElement is PsiCallExpression) {
                val parent = psiElement.getParent()
                if (parent != null && parent is PsiMethod) {
//                    val statements = PsiTreeUtil
//                            .findChildrenOfType(parent, PsiStatement::class.java)
//                    statements.forEach {
//                        val children = PsiTreeUtil
//                                .findChildrenOfType(it, PsiCallExpression::class.java)
//                        if (children.isNotEmpty()) {
//                            return children.first()
//                        }
                    selectedStatements.addAll(PsiTreeUtil.findChildrenOfType(parent, PsiStatement::class.java))
                    break
  //                  }
                } else {
//                    val psiStatement = PsiTreeUtil
//                            .getParentOfType(psiElement, PsiStatement::class.java)
//                    val children = PsiTreeUtil
//                            .findChildrenOfType(psiStatement, PsiCallExpression::class.java)
//                    if (children.isNotEmpty()) {
//                        return children.first()
//                    }
                    selectedStatements.add(PsiTreeUtil.getParentOfType<PsiStatement>(psiElement, PsiStatement::class.java) ?: continue)
                }
            }
        }
        selectedStatements.map { PsiTreeUtil.findChildrenOfType(it, PsiCallExpression::class.java) }.flatten()
        println(selectedStatements)
        if (selectedStatements.isNotEmpty()) {
            return selectedStatements.first() as PsiCallExpression
        }
        hintManager.showErrorHint(editor, "No expressions found in current selection")
        return null
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