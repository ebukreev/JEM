package org.jetbrains.research.jem.plugin

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

class KCallExtractor(private val psiFile: PsiFile,
                     private val startOffset: Int,
                     private val endOffset: Int) {

    fun extract(): Set<KtCallElement> {
        val selectedStatements = mutableSetOf<KtExpression>()
        for (i in startOffset..endOffset) {
            val psiElement = psiFile.findElementAt(i)
            if (psiElement != null) {
                val psiStatement = psiElement.getParentOfType<KtExpression>(false)
                if (psiStatement != null && !selectedStatements.contains(psiStatement)) {
                    selectedStatements.add(psiStatement)
                }
            }
        }
        return selectedStatements.map {
            it.getChildrenOfType<KtCallElement>().toSet()
        }.flatten().toSet()
    }

}

class JCallExtractor(private val psiFile: PsiFile,
                     private val startOffset: Int,
                     private val endOffset: Int) {

    fun extract(): Set<PsiCall> {
        val selectedStatements = mutableSetOf<PsiStatement>()
        for (i in startOffset..endOffset) {
            val psiElement = psiFile.findElementAt(i)
            if (psiElement != null) {
                val psiStatement = psiElement.getParentOfType<PsiStatement>(false)
                if (psiStatement != null && !selectedStatements.contains(psiStatement)) {
                    selectedStatements.add(psiStatement)
                }
            }
        }
        return selectedStatements.map {
            PsiTreeUtil.findChildrenOfType(it, PsiCall::class.java).toSet()
        }.flatten().toSet()
    }
}