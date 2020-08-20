package org.jetbrains.research.jem.plugin

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
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
import org.jetbrains.research.jem.plugin.JavaCaretAnalyzer.getJarPath
import org.jetbrains.research.jem.plugin.KotlinCaretAnalyzer.getJarPath
import org.jetbrains.research.jem.plugin.KotlinCaretAnalyzer.toJsonPath
import java.io.File

interface CaretAnalyzer {

    val hintManager: HintManager
        get() = HintManager.getInstance()

    fun analyze(psiFile: PsiFile, caret: Caret)
            : Map<PsiType, Set<Discovery>>?

    fun String.toJsonPath(): String =
            ".${File.separator}analyzedLibs${File.separator}${this
                    .substringAfterLast(File.separator)
                    .removeSuffix(".jar")}.json"

    fun <T> tryExtract(editor: Editor, method: () -> Set<T>): Set<T>? {
        val result: Set<T>
        try {
            result = method.invoke()
        } catch (mme: MultipleMethodException) {
            hintManager.showErrorHint(editor, "Please keep your selection within one method")
            return null
        }
        if (result.isEmpty()) {
            hintManager.showErrorHint(editor, "No expressions found in current selection")
            return null
        }
        return result
    }
}

object JavaCaretAnalyzer: CaretAnalyzer {
    override fun analyze(psiFile: PsiFile, caret: Caret): Map<PsiType, Set<Discovery>>? {
        val statementExtractor = PsiStatementExtractor(psiFile, caret.selectionStart, caret.selectionEnd)
        val methodCallExpressionExtractor = PsiMethodCallExpressionExtractor(statementExtractor)
        val editor = caret.editor
        val psiMethodCalls =
            tryExtract(editor) { methodCallExpressionExtractor.extract() } ?: return null
        return getDiscoveredExceptionsMap(psiMethodCalls, editor.project ?: return null)
    }

    fun descriptorFor(method: PsiMethod): String =
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
            if (method.notInJar()) {
                continue
            }
            val exceptions = getExceptionsFor(method, false)
            exceptions.forEach {
                val typeOfIt = PsiType.getTypeByName(it, project, call.resolveScope)
                val discovery = Discovery(typeOfIt, call, method)
                result.getOrPut(typeOfIt) { mutableSetOf() }.add(discovery)
            }
        }
        return result
    }

    fun PsiMethod.getJarPath() =
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
        val kCallExtractor = KCallElementExtractor(kStatementExtractor)
        val editor = caret.editor
        val psiMethodCalls =
                tryExtract(editor) { kCallExtractor.extract() } ?: return null
        return getDiscoveredExceptionsMap(psiMethodCalls, editor.project ?: return null)
    }

    fun CallableDescriptor.getJarPath(): String =
            this.findPsi()!!.containingFile.virtualFile.toString()
                .replaceAfterLast(".jar", "")
                .replaceBefore("://", "")
                .removePrefix("://")

    private fun getDiscoveredExceptionsMap(psiMethodCalls: Set<KtCallElement>, project: Project): Map<PsiType, Set<Discovery>> {
        val result = mutableMapOf<PsiType, MutableSet<Discovery>>()
        for (call in psiMethodCalls) {
            val method = call.getResolvedCall(call.analyze())?.resultingDescriptor ?: continue
            if (method.findPsi()?.notInJar() != false) {
                continue
            }
            val exceptions = getExceptionsFor(method, true)
            exceptions.forEach {
                val typeOfIt = PsiType.getTypeByName(it, project, call.resolveScope)
                val discovery = Discovery(typeOfIt, call, method.findPsi() as PsiMethod)
                result.getOrPut(typeOfIt) { mutableSetOf() }.add(discovery)
            }
        }
        return result
    }

    fun descriptorFor(method: CallableDescriptor): String =
            buildString {
                append("(")
                method.valueParameters.forEach {
                    append(Descriptor.of(it.source.getPsi()?.text?.replace(" classname", "")))
                }
                append(")")
                append(Descriptor.of(method.returnType.toClassDescriptor?.fqNameSafe.toString()))
            }
}

private fun PsiElement.notInJar(): Boolean =
        !(this.containingFile?.virtualFile.toString().startsWith("jar"))

private fun <T> getExceptionsFor(method: T, isKotlin: Boolean): Set<String> {
    val jarPath: String
    val name: String
    val `class`: String
    val descriptor: String
    if (isKotlin) {
        val m = method as CallableDescriptor
        jarPath = m.getJarPath()
        name = m.name.toString()
        `class` = m.containingDeclaration.fqNameSafe.toString()
        descriptor = KotlinCaretAnalyzer.descriptorFor(m)
    } else {
        val m = method as PsiMethod
        jarPath = m.getJarPath()
        name = m.name
        `class` = m.containingClass?.qualifiedName.toString()
        descriptor = JavaCaretAnalyzer.descriptorFor(m)
    }
    val jsonPath = jarPath.toJsonPath()
    val lib = InfoReader.read(jsonPath)
    return lib.classes
            .find { it.className == `class` }
            ?.methods
            ?.find { it.methodName == name && it.descriptor == descriptor }
            ?.exceptions ?: emptySet()
}