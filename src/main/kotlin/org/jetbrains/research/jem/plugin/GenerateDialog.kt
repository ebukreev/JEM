package org.jetbrains.research.jem.plugin

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.psi.PsiCallExpression
import com.intellij.psi.PsiFile
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.thomas.checkMate.presentation.exception_form.DefaultListDecorator
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel

class GenerateDialog(currentFile: PsiFile, callToExceptions: Pair<PsiCallExpression, Set<String>>,
                     name: String, klass: String) : DialogWrapper(currentFile.project) {

    private val exceptionsForm: ExceptionsForm

    init {
        title = "JVM Exceptions Manager"
        val list: JList<String> = JBList(callToExceptions.second)
        exceptionsForm = ExceptionsForm(list, name, klass)
        init()
    }

    override fun createButtonsPanel(buttons: List<JButton?>): JPanel {
        return layoutButtonsPanel(buttons.minus(buttons.elementAt(1)))
    }

    override fun createCenterPanel(): JComponent? {
        return exceptionsForm.splitter
    }
}

class ExceptionsForm(exceptionList: JList<String>, name: String, klass: String) {

    internal val splitter: JBSplitter

    init {
        val decoratedExceptionList = DefaultListDecorator<String>()
                .decorate(exceptionList, "Possible exceptions in method $name from class $klass:")
        exceptionList.selectedIndex = 0
        splitter = createSplitter(decoratedExceptionList)
    }

    private fun createSplitter(exceptionList: LabeledComponent<*>): JBSplitter {
        val jbSplitter = JBSplitter(false)
        jbSplitter.firstComponent = exceptionList
        return jbSplitter
    }
}