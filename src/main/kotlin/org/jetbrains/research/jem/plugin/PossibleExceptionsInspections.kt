package org.jetbrains.research.jem.plugin

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiTryStatement
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

private val ls = System.lineSeparator()

class JPossibleExceptionsInspection : AbstractBaseJavaLocalInspectionTool() {

    @NotNull
    override fun buildVisitor(@NotNull holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitTryStatement(statement: PsiTryStatement) {
                super.visitTryStatement(statement)
                val start = statement.tryBlock?.startOffset ?: return
                val end = statement.tryBlock?.endOffset ?: return
                val editor = statement.findExistingEditor() ?: return
                val exceptions =
                    JavaCaretAnalyzer.analyzeForInspection(statement.containingFile, editor, start, end)
                val caught = statement.catchSections.map { it.catchType!!.canonicalText }
                val possible = exceptions.minus(caught)
                if (possible.isNotEmpty()) {
                    holder.registerProblem(statement,
                            "Can also be thrown by this code block:$ls" +
                                    possible.joinToString(ls) { it }
                    )
                }
            }
        }
    }
}

class KPossibleExceptionsInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): KtVisitorVoid {

        return object : KtVisitorVoid() {
            override fun visitTryExpression(expression: KtTryExpression) {
                super.visitTryExpression(expression)
                val start = expression.startOffset
                val end = expression.tryBlock.endOffset
                val editor = expression.findExistingEditor() ?: return
                val exceptions =
                        KotlinCaretAnalyzer.analyzeForInspection(expression.containingFile, editor, start, end)
                val caught = expression.catchClauses.mapNotNull {
                    it.catchParameter?.type().toClassDescriptor?.fqNameSafe?.asString()
                }
                val possible = exceptions.minus(caught)
                if (possible.isNotEmpty()) {
                    holder.registerProblem(expression,
                            "Can also be thrown by this code block:$ls" +
                                    possible.joinToString(ls) { it }
                    )
                }
            }
        }
    }
}