package org.jetbrains.research.jem.plugin

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import com.thomas.checkMate.editing.MultipleMethodException
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.util.*

class ScopeTracker<T : KtElement>(private val classToTrack: Class<out KtElement>) {

    fun foundInScopes(psiElement: T, scopes: Collection<T>): Boolean {
        return scopes
                .map { mce -> PsiTreeUtil.findChildrenOfType(mce, classToTrack) }
                .any { c -> c == psiElement }
    }

    fun removeSmallerScopes(psiElement: T, scopes: MutableCollection<T>) {
        val children =
                PsiTreeUtil.findChildrenOfType(psiElement, classToTrack)
        scopes.removeAll(children)
    }
}

class KtExpressionExtractor(private val psiFile: KtFile,
                            private val startOffset: Int,
                            private val endOffset: Int) {

    private val scopeTracker: ScopeTracker<KtExpression> =
            ScopeTracker(KtExpression::class.java)
    private var extractionMethod: KtFunction? = null

    fun extract(): List<KtExpression> {
        var selectedStatements: MutableList<KtExpression> = ArrayList()
        var methodFound = false
        for (i in startOffset..endOffset) {
            val psiElement = psiFile.findElementAt(i)
            checkMultiple(psiElement)
            if (!methodFound && psiElement != null) {
                val parent = psiElement.parent
                if (parent != null && parent is KtFunction) {
                    selectedStatements = getMethodStatements(parent)
                    methodFound = true
                } else {
                    val psiStatement = psiElement.getParentOfType<KtExpression>(false)
                    addToStatements(psiStatement, selectedStatements)
                }
            }
        }
        return selectedStatements
    }

    private fun checkMultiple(psiElement: PsiElement?) {
        val currentMethod = psiElement?.getParentOfType<KtFunction>(false)
        if (extractionMethod == null) {
            extractionMethod = currentMethod
        } else {
            if (extractionMethod != currentMethod) {
                throw MultipleMethodException("Can't extract statements from multiple methods")
            }
        }
    }

    private fun getMethodStatements(psiMethod: KtFunction): MutableList<KtExpression> {
        val selectedStatements: MutableList<KtExpression> = ArrayList()
        val psiStatements = psiMethod.getChildrenOfType<KtExpression>()
        selectedStatements.addAll(psiStatements)
        return selectedStatements
    }

    private fun addToStatements(psiStatement :KtExpression?, selectedStatements: MutableList<KtExpression>) {
        if (psiStatement != null && !selectedStatements.contains(psiStatement) && !scopeTracker.foundInScopes(psiStatement, selectedStatements)) {
            selectedStatements.add(psiStatement)
            scopeTracker.removeSmallerScopes(psiStatement, selectedStatements)
        }
    }
}

class KPsiMethodCallExpressionExtractor(psiStatementExtractor: KtExpressionExtractor) {
    private val ktExpressionExtractor: KtExpressionExtractor = psiStatementExtractor
    private val scopeTracker = ScopeTracker<KtCallElement>(KtCallElement::class.java)

    fun extract(): Set<KtCallElement> {
        val selectedExpressions: MutableSet<KtCallElement> = HashSet()
        ktExpressionExtractor.extract().forEach { s ->
            val psiCallExpressions = s.getChildrenOfType<KtCallElement>()
            psiCallExpressions
                    .filter { e -> !scopeTracker.foundInScopes(e, selectedExpressions) }
                    .forEach { e -> selectedExpressions.add(e) }
        }
        return selectedExpressions
    }
}