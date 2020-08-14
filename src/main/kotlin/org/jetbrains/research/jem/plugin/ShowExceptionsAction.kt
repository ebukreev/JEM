package org.jetbrains.research.jem.plugin

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.PsiCallExpression
import com.thomas.checkMate.editing.MultipleMethodException
import com.thomas.checkMate.editing.PsiMethodCallExpressionExtractor
import com.thomas.checkMate.editing.PsiStatementExtractor
import java.util.*

class ShowExceptionsAction : AnAction() {

    private val hintManager: HintManager = HintManager.getInstance()

    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (psiFile == null || editor == null) {
            return
        }
        val currentCaret = editor.caretModel.currentCaret
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
        val exceptionFinder: MutableMap<PsiCallExpression, Set<String>> = HashMap()
        for (f in psiMethodCalls) {
            val name = f.resolveMethod()?.name
            val `class` = f.resolveMethod()?.containingClass?.qualifiedName
            exceptionFinder[f] = setOf("$name, $`class`", "hi")
        }
        val generateDialog = GenerateDialog(psiFile, exceptionFinder)
        generateDialog.show()
        currentCaret.removeSelection()
        psiFile.navigate(true)
    }
}