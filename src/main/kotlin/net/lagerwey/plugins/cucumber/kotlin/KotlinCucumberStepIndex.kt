package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.impl.source.tree.RecursiveLighterASTNodeWalkingVisitor
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.PsiDependentFileContent
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.plugins.cucumber.CucumberStepIndex

class KotlinCucumberStepIndex : CucumberStepIndex() {
    private val inputFilter = DefaultFileTypeSpecificInputFilter(KotlinFileType.INSTANCE)

    override fun getName(): ID<Boolean, MutableList<Int>> = INDEX_ID

    override fun getVersion() = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter = inputFilter

    override fun getPackagesToScan(): Array<String> = arrayOf(cucumberPackage)

    override fun getIndexer(): DataIndexer<Boolean, MutableList<Int>, FileContent> {
        // Override to support steps defined in subclasses
        return DataIndexer { inputData ->
            val text = inputData.contentAsText
            val lighterAst = (inputData as PsiDependentFileContent).lighterAST
            mapOf(true to getStepDefinitionOffsets(lighterAst, text))
        }
    }

    override fun getStepDefinitionOffsets(lighterAst: LighterAST, text: CharSequence): MutableList<Int> {
        val results = mutableListOf<Int>()

        val visitor = object : RecursiveLighterASTNodeWalkingVisitor(lighterAst) {
            override fun visitNode(element: LighterASTNode) {
                if (element.tokenType == KtNodeTypes.CALL_EXPRESSION) {
                    val methodAndArguments = lighterAst.getChildren(element)

                    if (methodAndArguments.size < 2) {
                        super.visitNode(element)
                        return
                    }

                    val gherkinMethod = methodAndArguments[0]
                    if (gherkinMethod != null && isStepDefinitionCall(gherkinMethod, text)) {
                        val expression = methodAndArguments[1]
                        if (expression.tokenType == KtNodeTypes.VALUE_ARGUMENT_LIST) {
                            lighterAst.getChildren(expression).find { it.tokenType == KtNodeTypes.VALUE_ARGUMENT }?.let {
                                val regex = text.subSequence(it.startOffset, it.endOffset)
                                if (regex.isNotEmpty() && regex != "\"\"") {
                                    results.add(element.startOffset)
                                }
                            }
                        }
                    }
                }
                super.visitNode(element);
            }
        }
        visitor.visitNode(lighterAst.root)

        return results
    }

    companion object {
        val INDEX_ID = ID.create<Boolean, MutableList<Int>>("kotlin.cucumber.step")
    }
}
