package org.jetbrains.research.jem.plugin

import com.intellij.codeInspection.*
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

private val ls = System.lineSeparator()

class JPossibleExceptionsInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun buildVisitor(@NotNull holder: ProblemsHolder, isOnTheFly: Boolean): JavaElementVisitor {
        return object : JavaElementVisitor() {

            override fun visitTryStatement(statement: PsiTryStatement) {
                super.visitTryStatement(statement)
                val start = statement.tryBlock?.startOffset ?: return
                val end = statement.tryBlock?.endOffset ?: return
                val exceptions =
                    JavaCaretAnalyzer.analyzeForInspection(statement.containingFile, statement.project, start, end)
                            .toMutableSet()
                val caught = statement.catchSections.map { it.catchType!!.canonicalText }
                caught.forEach { c ->
                    val forRemove = mutableListOf<String>()
                    exceptions.forEach { e ->
                        if (PsiType.getTypeByName(c, statement.project, statement.resolveScope)
                                        .isAssignableFrom(PsiType
                                                .getTypeByName(e, statement.project, statement.resolveScope))) {
                                            forRemove.add(e)
                                        }
                    }
                    exceptions.removeAll(forRemove)
                }
                if (exceptions.isNotEmpty()) {
                    holder.registerProblem(statement,
                            "Can also be thrown by this code block:$ls" +
                                    exceptions.joinToString(ls) { it }
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
                val exceptions =
                        KotlinCaretAnalyzer.analyzeForInspection(expression.containingFile, expression.project, start, end)
                                .toMutableSet()
                val caught = expression.catchClauses.mapNotNull {
                    it.catchParameter?.type().toClassDescriptor?.fqNameSafe?.asString()
                }
                caught.forEach { c ->
                    val forRemove = mutableListOf<String>()
                    exceptions.forEach { e ->
                        if (PsiType.getTypeByName(c, expression.project, expression.resolveScope)
                                        .isAssignableFrom(PsiType
                                                .getTypeByName(e, expression.project, expression.resolveScope))) {
                            forRemove.add(e)
                        }
                    }
                    exceptions.removeAll(forRemove)
                }
                if (exceptions.isNotEmpty()) {
                    holder.registerProblem(expression,
                            "Can also be thrown by this code block:$ls" +
                                    exceptions.joinToString(ls) { it }
                    )
                }
            }
        }
    }
}

class PossibleExceptionsGlobalInspection : GlobalSimpleInspectionTool() {
    override fun checkFile(file: PsiFile,
                           manager: InspectionManager,
                           problemsHolder: ProblemsHolder,
                           globalContext: GlobalInspectionContext,
                           problemDescriptionsProcessor: ProblemDescriptionsProcessor) {
        val isKotlin = when (file.language) {
            is JavaLanguage -> false
            is KotlinLanguage -> true
            else -> return
        }
        acceptAll(file, problemsHolder, isKotlin)
    }

    private fun acceptAll(elem: PsiElement, holder: ProblemsHolder, isKotlin: Boolean) {
        if (isKotlin && elem is KtTryExpression) {
            KPossibleExceptionsInspection()
                    .buildVisitor(holder, false).visitTryExpression(elem)
        } else if (!isKotlin && elem is PsiTryStatement) {
            JPossibleExceptionsInspection()
                    .buildVisitor(holder, false).visitTryStatement(elem)
        }
        elem.children.forEach { acceptAll(it, holder, isKotlin) }
    }
}