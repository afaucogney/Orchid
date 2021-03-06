package com.eden.orchid.strikt

import com.eden.orchid.utilities.applyIf
import org.jsoup.Jsoup
import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

// Prep trees for evaluation
//----------------------------------------------------------------------------------------------------------------------

internal fun String.normalizeDoc(removeComments: Boolean = true): Document {
    return Jsoup.parse(this)
        .apply {
            outputSettings(Document.OutputSettings().apply {
                indentAmount(2)
                prettyPrint(true)
                outline(true)
            })

            if (removeComments) {
                filter(CommentFilter)
            }
        }
}

// Evaluate trees for similarity
//----------------------------------------------------------------------------------------------------------------------

internal fun List<Node>.hasHtmlSimilarTo(expected: List<Node>, ignoreContentWhitespace: Boolean): Boolean {
    val actualThis = this.filter { it !is TextNode || !it.isBlank }
    val actualExpected = expected.filter { it !is TextNode || !it.isBlank }

    // both these sets are empty, so they are equal
    if (actualThis.isEmpty() && actualExpected.isEmpty()) {
        return true
    }

    // sets have different sizes, it's easy to tell they are not equal
    if (actualThis.size != actualExpected.size) {
        return false
    }

    actualThis.forEachIndexed { index, thisElement ->
        val expectedElement = actualExpected[index]

        if (thisElement is TextNode) {
            if (expectedElement !is TextNode ) {
                return@hasHtmlSimilarTo false
            }
            else if (thisElement.text().trim().trimLines() != expectedElement.text().trim().trimLines()) {
                return@hasHtmlSimilarTo false
            }
        } else if (thisElement is Element) {
            if (expectedElement !is Element || !thisElement.hasHtmlSimilarTo(expectedElement, ignoreContentWhitespace)) {
                return@hasHtmlSimilarTo false
            }
        }
    }

    return true
}

private fun Element.hasHtmlSimilarTo(expected: Element, ignoreContentWhitespace: Boolean): Boolean {
    // check tag name
    val tagName = { it: Element -> it.tagName() }
    val thisTagName = tagName(this)
    val expectedTagName = tagName(expected)
    if (thisTagName != expectedTagName) {
        return false
    }

    // check own text content
    val ownTextContent = { it: Element -> it.ownText().trim().trimLines() }
    val thisOwnTextContent = ownTextContent(this).applyIf(ignoreContentWhitespace) { replace("\\s+".toRegex(), "") }
    val expectedOwnTextContent = ownTextContent(expected).applyIf(ignoreContentWhitespace) { replace("\\s+".toRegex(), "") }
    if (thisOwnTextContent != expectedOwnTextContent) {
        return false
    }

    // check classes
    val ownClasses = { it: Element -> it.classNames().sorted().distinct() }
    val thisOwnClasses = ownClasses(this)
    val expectedOwnClasses = ownClasses(expected)
    if (thisOwnClasses != expectedOwnClasses) {
        return false
    }

    // check attributes
    val attributes = { it: Element -> it.attributes() }
    val thisAttributes = attributes(this)
    val expectedAttributes = attributes(expected)
    if (!thisAttributes.hasAttributesSimilarTo(expectedAttributes)) {
        return false
    }

    // check child notes
    if (!this.childNodes().hasHtmlSimilarTo(expected.childNodes(), ignoreContentWhitespace)) {
        return false
    }

    return true
}

private fun Attributes.hasAttributesSimilarTo(expected: Attributes): Boolean {
    // sets have different sizes, it's easy to tell they are not equal
    if (this.size() != expected.size()) {
        return false
    }

    // check all the props in `this` match those in `expected`
    this.forEach { thisAttribute ->
        if (thisAttribute.key != "class") {
            val actualAttributeKey = thisAttribute.key.removePrefix("data-")

            val expectedKeyExists = expected.hasKey(actualAttributeKey) || expected.hasKey("data-$actualAttributeKey")
            val expectedAttribute = if (expected.hasKey(actualAttributeKey)) expected.get(thisAttribute.key) else expected.get("data-$actualAttributeKey")

            // expected tag does not have a key
            if (!expectedKeyExists) {
                return@hasAttributesSimilarTo false
            }
            else if(thisAttribute.value.isBlank()) {
                // likely a boolean property, sometimes it is empty string, sometimes has value same as name
                if(!expectedAttribute.isBlank() && !listOf(thisAttribute.key, "data-$actualAttributeKey").any { it == expectedAttribute }) {
                    return@hasAttributesSimilarTo false
                }
            }
            else if(thisAttribute.value != expectedAttribute) {
                // actual property, must match
                return@hasAttributesSimilarTo false
            }
        }
    }

    return true
}
