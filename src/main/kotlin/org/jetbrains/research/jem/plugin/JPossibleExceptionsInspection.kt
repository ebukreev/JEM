package org.jetbrains.research.jem.plugin

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset


class JPossibleExceptionsInspection : AbstractBaseJavaLocalInspectionTool() {

    @NotNull
    override fun buildVisitor(@NotNull holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitTryStatement(statement: PsiTryStatement?) {
                super.visitTryStatement(statement)
                if (statement == null) return
                val start = statement.tryBlock?.startOffset ?: return
                val end = statement.tryBlock?.endOffset ?: return
                val editor = statement.findExistingEditor() ?: return
                val exceptions =
                    JavaCaretAnalyzer.analyze(statement.containingFile, editor, start, end)?.keys ?: return
                val caught = statement.catchSections.map { it.catchType }
                val possible = exceptions.minus(caught)
                if (possible.isNotEmpty()) {
                    holder.registerProblem(statement, possible.map{ it?.canonicalText }.joinToString())
                }
            }
        }
    }
}