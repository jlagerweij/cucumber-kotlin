package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.impl.source.tree.RecursiveLighterASTNodeWalkingVisitor
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.PsiDependentFileContent
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.KotlinFileType

val PARAMETER_TYPE_INDEX_ID = ID.create<String, String>("kotlin.cucumber.parameterType")

private const val PARAMETER_TYPE_METHOD = "ParameterType"

/**
 * Indexes the custom parameter types declared with `ParameterType("name", "regex") { ... }` in Kotlin files,
 * mapping the parameter type name to its regex.
 *
 * Like [CucumberKotlinLambdaStepIndex], this only looks at the syntax, so declarations in subclasses of a
 * cucumber-java8 glue interface are found without resolving references.
 */
class CucumberKotlinParameterTypeIndex : FileBasedIndexExtension<String, String>() {

    override fun getName(): ID<String, String> = PARAMETER_TYPE_INDEX_ID

    override fun getVersion() = 1

    override fun dependsOnFileContent() = true

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(KotlinFileType.INSTANCE)

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer() = EnumeratorStringDescriptor.INSTANCE

    override fun getIndexer(): DataIndexer<String, String, FileContent> = DataIndexer { inputData ->
        val text = inputData.contentAsText
        if (!text.contains(PARAMETER_TYPE_METHOD)) {
            emptyMap()
        } else {
            findParameterTypes((inputData as PsiDependentFileContent).lighterAST, text)
        }
    }

    private fun findParameterTypes(lighterAst: LighterAST, text: CharSequence): Map<String, String> {
        val parser = ParameterTypeParser(lighterAst, text)
        val parameterTypes = mutableMapOf<String, String>()
        val visitor = object : RecursiveLighterASTNodeWalkingVisitor(lighterAst) {
            override fun visitNode(element: LighterASTNode) {
                if (element.tokenType == KtNodeTypes.CALL_EXPRESSION) {
                    parser.toParameterType(element)?.let { parameterTypes.putIfAbsent(it.first, it.second) }
                }
                super.visitNode(element)
            }
        }
        visitor.visitNode(lighterAst.root)
        return parameterTypes
    }

    /**
     * Reads `ParameterType("name", "regex", ...)` calls from the lighter AST of a Kotlin file.
     */
    private class ParameterTypeParser(private val lighterAst: LighterAST, private val text: CharSequence) {

        /**
         * Returns the name and regex of a `ParameterType("name", "regex", ...)` call, or `null` for any other call.
         */
        fun toParameterType(call: LighterASTNode): Pair<String, String>? {
            val children = lighterAst.getChildren(call)
            if (children.firstOrNull()?.let(::textOf) != PARAMETER_TYPE_METHOD) return null

            val argumentList = children.firstOrNull { it.tokenType == KtNodeTypes.VALUE_ARGUMENT_LIST } ?: return null
            val arguments = lighterAst.getChildren(argumentList).filter { it.tokenType == KtNodeTypes.VALUE_ARGUMENT }
            val name = arguments.getOrNull(0)?.let(::stringArgument) ?: return null
            val regex = arguments.getOrNull(1)?.let(::stringArgument) ?: return null
            return name to StringEscapeUtils.unescapeJava(regex)
        }

        /**
         * Returns the content of a string literal argument without its quotes, or `null` if the argument is not a string literal.
         */
        private fun stringArgument(argument: LighterASTNode): String? {
            val template = lighterAst.getChildren(argument).singleOrNull { it.tokenType == KtNodeTypes.STRING_TEMPLATE }
            return template?.let(::textOf)?.let(::unquote)
        }

        private fun textOf(node: LighterASTNode) = text.subSequence(node.startOffset, node.endOffset).toString()

        private fun unquote(literal: String): String? {
            val quote = if (literal.startsWith(RAW_QUOTE)) RAW_QUOTE else QUOTE
            val isQuoted = literal.length >= 2 * quote.length && literal.startsWith(quote) && literal.endsWith(quote)
            return if (isQuoted) literal.substring(quote.length, literal.length - quote.length) else null
        }
    }

    private companion object {
        const val QUOTE = "\""
        const val RAW_QUOTE = "\"\"\""
    }
}
