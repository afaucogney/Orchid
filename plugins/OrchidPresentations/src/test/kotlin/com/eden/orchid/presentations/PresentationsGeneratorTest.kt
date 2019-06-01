package com.eden.orchid.presentations

import com.eden.orchid.impl.generators.HomepageGenerator
import com.eden.orchid.testhelpers.OrchidIntegrationTest
import com.eden.orchid.testhelpers.TestGeneratorModule
import com.eden.orchid.testhelpers.asHtml
import com.eden.orchid.testhelpers.matches
import com.eden.orchid.testhelpers.pageWasRendered
import com.eden.orchid.testhelpers.select
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize

@DisplayName("Tests behavior of Presentations generator")
class PresentationsGeneratorTest : OrchidIntegrationTest(PresentationsModule(), TestGeneratorModule(HomepageGenerator::class.java)) {

    @Test
    @DisplayName("Tests that presentations are set up correctly based on Markdown files in the `presentations` directory.")
    fun test01() {
        resource(
            "homepage.md",
            "",
            """
            |{
            |  "components": [
            |    {
            |      "type": "presentation",
            |       "presentation": "test-presentation"
            |    }
            |  ],
            |  "extraJs": ["https://code.jquery.com/jquery-3.4.1.min.js"]
            |}
            """.trimMargin()
        )

        resource("presentations/test-presentation/slide-1.md", "Slide 1")
        resource("presentations/test-presentation/slide-2.md", "Slide 2")
        resource("presentations/test-presentation/slide-3.md", "Slide 3")

        val testResults = execute()
        expectThat(testResults).pageWasRendered("//index.html")
            .get { content }
            .asHtml()
            .and {
                select(".deck-container")
                    .matches()
                    .hasSize(1)
            }
            .and {
                select(".slide")
                    .matches()
                    .hasSize(3)
            }
    }

}
