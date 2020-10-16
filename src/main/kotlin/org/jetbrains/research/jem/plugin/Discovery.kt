package org.jetbrains.research.jem.plugin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType


data class Discovery(
    val exceptionType: PsiType,
    val indicator: PsiElement,
    val encapsulatingMethod: PsiMethod
)