package org.jetbrains.research.jem.plugin

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import org.jetbrains.kotlin.idea.KotlinLanguage

class ShowExceptionsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(LangDataKeys.PSI_FILE) ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val currentCaret = editor.caretModel.currentCaret
        val discoveredExceptions =
                when (psiFile.language) {
                    is KotlinLanguage ->
                        KotlinCaretAnalyzer.analyze(
                                psiFile,
                                e.project!!,
                                currentCaret.selectionStart,
                                currentCaret.selectionEnd
                            )
                    is JavaLanguage ->
                        JavaCaretAnalyzer.analyze(
                            psiFile,
                            e.project!!,
                            currentCaret.selectionStart,
                            currentCaret.selectionEnd
                        )
                    else -> null
                } ?: return
        val generateDialog = GenerateDialog(discoveredExceptions, psiFile)
        generateDialog.show()
        if (generateDialog.isOK) {
            currentCaret.removeSelection()
        }
        psiFile.navigate(true)
    }
}