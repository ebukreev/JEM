package org.jetbrains.research.jem.plugin

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.project.Project
import com.intellij.psi.*
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
import org.jetbrains.research.jem.analysis.MethodAnalyzer
import org.jetbrains.research.jem.interaction.*
import org.jetbrains.research.jem.plugin.JavaCaretAnalyzer.getJarPath
import org.jetbrains.research.jem.plugin.KotlinCaretAnalyzer.getJarPath
import java.io.File
import java.io.FileNotFoundException

interface CaretAnalyzer {

    fun analyze(psiFile: PsiFile, caret: Caret)
            : Map<PsiType, Set<Discovery>>?

    fun analyzeForInspection(psiFile: PsiFile, project: Project, startOffset: Int, endOffset: Int)
            : Set<String>
}

object JavaCaretAnalyzer: CaretAnalyzer {

    override fun analyze(psiFile: PsiFile, caret: Caret): Map<PsiType, Set<Discovery>> =
        getDiscoveredExceptionsMap(
            JCallExtractor(
                psiFile,
                caret.selectionStart,
                caret.selectionEnd
            ).extract(),
            caret.editor.project
        )

    override fun analyzeForInspection(psiFile: PsiFile, project: Project, startOffset: Int, endOffset: Int)
            : Set<String> =
        getDiscoveredExceptionsMap(JCallExtractor(psiFile, startOffset, endOffset).extract(), project)
                .keys.map { it.canonicalText }.toSet()

    fun descriptorFor(method: PsiMethod): String =
            buildString {
                append("(")
                method.parameterList.parameters.forEach {
                    append(Descriptor.of(it.type.canonicalText))
                }
                append(")")
                append(Descriptor.of(method.returnType?.canonicalText) ?: "")
            }

    private fun getDiscoveredExceptionsMap(psiMethodCalls: Set<PsiCall>, project: Project?)
            : Map<PsiType, Set<Discovery>> {
        val result = mutableMapOf<PsiType, MutableSet<Discovery>>()
        if (project == null)
            return emptyMap()
        if (!LazyPolymorphAnalyzer.isInit()) {
            LazyPolymorphAnalyzer.init(project)
        }
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

    override fun analyze(psiFile: PsiFile, caret: Caret): Map<PsiType, Set<Discovery>> =
        getDiscoveredExceptionsMap(
            KCallExtractor(
                psiFile as KtFile,
                caret.selectionStart,
                caret.selectionEnd).extract(),
            caret.editor.project
        )

    override fun analyzeForInspection(psiFile: PsiFile, project: Project, startOffset: Int, endOffset: Int)
            : Set<String> =
        getDiscoveredExceptionsMap(KCallExtractor(psiFile as KtFile, startOffset, endOffset).extract(), project)
                .keys.map { it.canonicalText }.toSet()

    fun CallableDescriptor.getJarPath(): String =
            this.findPsi()!!.containingFile.virtualFile.toString()
                .replaceAfterLast(".jar", "")
                .replaceBefore("://", "")
                .removePrefix("://")

    private fun getDiscoveredExceptionsMap(psiMethodCalls: Set<KtCallElement>, project: Project?)
            : Map<PsiType, Set<Discovery>> {
        val result = mutableMapOf<PsiType, MutableSet<Discovery>>()
        if (project == null)
            return emptyMap()
        if (!LazyPolymorphAnalyzer.isInit()) {
            LazyPolymorphAnalyzer.init(project)
        }
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
                    append(Descriptor.of(it.source.getPsi()?.text?.replace(" classname", "")) ?: "")
                }
                append(")")
                append(Descriptor.of(method.returnType.toClassDescriptor?.fqNameSafe.toString()) ?: "")
            }
}

private fun PsiElement.notInJar(): Boolean =
        !(this.containingFile?.virtualFile.toString().startsWith("jar"))

private fun <T> getExceptionsFor(method: T, isKotlin: Boolean): Set<String> {
    val jarPath: String
    val name: String
    val clazz: String
    val descriptor: String
    if (isKotlin) {
        val m = method as CallableDescriptor
        jarPath = m.getJarPath()
        name = m.name.toString()
        clazz = m.containingDeclaration.fqNameSafe.toString()
        descriptor = KotlinCaretAnalyzer.descriptorFor(m)
    } else {
        val m = method as PsiMethod
        jarPath = m.getJarPath()
        name = m.name
        clazz = m.containingClass?.qualifiedName.toString()
        descriptor = JavaCaretAnalyzer.descriptorFor(m)
    }
    val jsonPath = System.getProperty("user.home") +
            "/.JEMPluginCache/" +
            clazz
                .replace("(\\.[A-Z].*)".toRegex(), "")
                .replace(".", "/") +
            "/${clazz.replace("(\\.[A-Z].*)".toRegex(), "")}.json"
    if (!File(jsonPath).exists()) {
        MethodAnalyzer.polyMethodsExceptions =
            emptyMap<MethodInformation, Set<String>>().toMutableMap()
        val methodInfo = MethodInformation(clazz, name, descriptor)
        val exceptions = LazyPolymorphAnalyzer.analyze(methodInfo)
        if (exceptions != null)
            return exceptions
        JarAnalyzer.analyze(jarPath, false)
    }

    val pack = InfoReader.read(jsonPath)
    return allInfoForExceptions(pack, clazz, name, descriptor)
}

private fun allInfoForExceptions(
    pack: Package,
    clazz: String,
    name: String,
    descriptor: String
): Set<String> {
    val result = mutableSetOf<String>()
    val exceptionsInfo = pack.classes
        .find { it.className == clazz }
        ?.methods
        ?.find { it.methodName == name && it.descriptor == descriptor }
        ?.exceptionsInfo ?: return emptySet()

    for (exception in exceptionsInfo.exceptions) {
        result.add(
            buildString {
                append("$clazz $name")
                append(" :")
                append(System.lineSeparator())
                append(exception)
            }
        )
    }





    for (call in exceptionsInfo.calls) {
        result.addAll(buildCallTrace(call.key.toMethodInfo(), getExceptionsInfoFor(call.key),"$clazz $name", call.value))
    }

    return result
}

fun getExceptionsInfoFor(call: String): ExceptionsAndCalls {
    val methodInfo = call.toMethodInfo()
    val jsonPath = System.getProperty("user.home") +
            "/.JEMPluginCache/" +
            methodInfo.clazz!!
                .replace("(\\.[A-Z].*)".toRegex(), "")
                .replace(".", "/") +
            "/${methodInfo.clazz.replace("(\\.[A-Z].*)".toRegex(), "")}.json"

        val packOfThisCall =
            try { InfoReader.read(jsonPath)
    } catch (e: FileNotFoundException) {
        return MethodAnalyzer(
            MethodAnalyzer
                .classPool
                .getCtClass(call.toMethodInfo().clazz)
                .getMethod(call.toMethodInfo()
                    .name, call.toMethodInfo().descriptor)
        ).getPossibleExceptions()
    }
    return packOfThisCall.classes
        .find { it.className == methodInfo.clazz }
        ?.methods
        ?.find { it.methodName == methodInfo.name && it.descriptor == methodInfo.descriptor }
        ?.exceptionsInfo ?: return ExceptionsAndCalls.empty()
}

fun buildCallTrace(call: MethodInformation, info: ExceptionsAndCalls, prevTrace: String, caught: Set<String>): Set<String> {
    val traces = mutableSetOf<String>()
    for (exception in info.exceptions) {
        if (exception !in caught) {
            traces.add(prevTrace + buildString {
                append(" ->")
                append(System.lineSeparator())
                append("${call.clazz} ${call.name}")
                append(" :")
                append(System.lineSeparator())
                append(exception)
            })
        }
    }
    for (callInfo in info.calls) {
        traces.addAll(buildCallTrace(callInfo.key.toMethodInfo(), getExceptionsInfoFor(callInfo.key), prevTrace, callInfo.value))
    }
    return traces
}