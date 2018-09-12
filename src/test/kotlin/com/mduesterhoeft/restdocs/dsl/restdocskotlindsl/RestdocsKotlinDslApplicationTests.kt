package com.mduesterhoeft.restdocs.dsl.restdocskotlindsl

import com.mduesterhoeft.restdocs.dsl.restdocskotlindsl.RestdocsDocumentationDsl.Companion.document
import org.junit.Rule
import org.junit.Test
import org.springframework.hateoas.Link
import org.springframework.hateoas.Link.REL_SELF
import org.springframework.hateoas.Resource
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.ResponseEntity
import org.springframework.restdocs.JUnitRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


class RestdocsKotlinDslApplicationTests {

	@Rule @JvmField
	public var restDocumentation = JUnitRestDocumentation()

	val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(TestController())
			.apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
			.build()

	@Test
	fun dslSample() {
		mockMvc.perform(post("/things")
				.header(CONTENT_TYPE, APPLICATION_JSON)
				.header(ACCEPT, APPLICATION_JSON)
				.content("""{"some": "value", "more": 12}"""))
                .andExpect(status().isOk())
				.andDo(print())
				.andDo(document("some") {
					request {
						field("some") { description("some") }
						field("more") { description("more")}
						header(CONTENT_TYPE) { description("some") }
					}
					response {
						field("some") { description("some") }
						field("more") { description("more")}
						link("self") { description("self link") }
					}
				})
	}

    @RestController
	@RequestMapping("/things")
	class TestController {

		@PostMapping
		fun doThings(@RequestBody thing: Thing): ResponseEntity<Resource<Thing>> {
			return ResponseEntity.ok(Resource(thing).apply { add(Link(REL_SELF, "http://localhost/1")) })
		}

		data class Thing(val some: String, val more: Int)
	}

}
