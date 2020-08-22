package org.jetbrains.research.jem.plugin

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiType
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.thomas.checkMate.discovery.general.Discovery
import com.thomas.checkMate.presentation.exception_form.DefaultListDecorator
import com.thomas.checkMate.presentation.exception_form.ExceptionIndicatorCellRenderer
import com.thomas.checkMate.presentation.exception_form.PsiTypeCellRenderer
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class GenerateDialog(discoveredExceptionMap: Map<PsiType, Set<Discovery>>, currentFile: PsiFile)
                     : DialogWrapper(currentFile.project) {

    private val exceptionsForm: JemExceptionsForm

    init {
        title = "JVM Exceptions Manager"
        exceptionsForm = JemExceptionsForm(discoveredExceptionMap, currentFile)
        init()
    }

    override fun createButtonsPanel(buttons: List<JButton?>): JPanel {
        return layoutButtonsPanel(buttons.minus(buttons.elementAt(1)))
    }

    override fun createCenterPanel(): JComponent? {
        return exceptionsForm.splitter
    }
}

class JemExceptionsForm(private var discoveredExceptionMap: Map<PsiType, Set<Discovery>>,
                        private val currentFile: PsiFile) {

    private var exceptionList: JList<PsiType>
    private var methodList: JList<Discovery>
    internal var splitter: JBSplitter
    private var currentActive: PsiElement

    init {
        currentActive = currentFile
        exceptionList = createExceptionList(discoveredExceptionMap.keys)
        methodList = createMethodList()
        val decoratedExceptionList = DefaultListDecorator<PsiType>()
                .decorate(exceptionList, "Possible exceptions")
        val decoratedMethodList = DefaultListDecorator<Discovery>()
                .decorate(methodList, "Inspect methods that throw this exception")
        exceptionList.selectedIndex = 0
        splitter = createSplitter(decoratedExceptionList, decoratedMethodList)
    }

    private fun createExceptionList(exceptionTypes: Set<PsiType>): JList<PsiType> {
        val exceptionList = JBList<PsiType>()
        val listModel = DefaultListModel<PsiType>()
        listModel.addAll(exceptionTypes)
        exceptionList.model = listModel
        exceptionList.addListSelectionListener {
            populateMethodListForSelectedExceptionWithIndex(exceptionList.leadSelectionIndex)
            activateIfNecessary(currentFile)
        }
        exceptionList.cellRenderer = PsiTypeCellRenderer()
        return exceptionList
    }

    private fun createMethodList(): JList<Discovery> {
        val methodList = JBList<Discovery>()
        methodList.cellRenderer = ExceptionIndicatorCellRenderer()
        return methodList
    }

    private fun createSplitter(exceptionList: LabeledComponent<*>, psiMethodList: LabeledComponent<*>): JBSplitter {
        val jbSplitter = JBSplitter(false)
        jbSplitter.firstComponent = exceptionList
        jbSplitter.secondComponent = psiMethodList
        jbSplitter.proportion = 0.5f
        return jbSplitter
    }

    private fun populateMethodListForSelectedExceptionWithIndex(index: Int) {
        if (index >= 0) {
            val psiType = exceptionList.model.getElementAt(index)
            val methodListModel = DefaultListModel<Discovery>()
            methodListModel.addAll(discoveredExceptionMap[psiType])
            methodList.model = methodListModel
            methodList.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val indicator = methodList.selectedValue
                    if (indicator != null) {
                        if (!e.isPopupTrigger) {
                            if (e.modifiersEx != InputEvent.SHIFT_DOWN_MASK) {
                                activateIfNecessary(indicator.indicator)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun activateIfNecessary(element: PsiElement?) {
        if (currentActive != element) {
            NavigationUtil.activateFileWithPsiElement(element!!)
            currentActive = element
        }
    }
}