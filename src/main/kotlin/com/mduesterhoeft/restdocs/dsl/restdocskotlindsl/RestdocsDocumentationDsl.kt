package com.mduesterhoeft.restdocs.dsl.restdocskotlindsl

import org.springframework.restdocs.headers.HeaderDescriptor
import org.springframework.restdocs.headers.HeaderDocumentation
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.hypermedia.HypermediaDocumentation
import org.springframework.restdocs.hypermedia.LinkDescriptor
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.RequestDocumentation
import org.springframework.restdocs.snippet.AbstractDescriptor
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.web.servlet.ResultHandler
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class RestdocsDocumentationDsl {


companion object {
    fun document(operationId: String, documentationFunction: DocumentationContainer.() -> Unit): ResultHandler =
            DocumentationContainer()
                    .apply(documentationFunction)
                    .let { MockMvcRestDocumentation.document(operationId, *toSnippets(it).toTypedArray())
                    }

    private val requestSnippetMapping = mutableListOf(
            SnippetFactory(FieldDescriptor::class, { l -> PayloadDocumentation.requestFields(l) }),
            SnippetFactory(HeaderDescriptor::class, { l -> HeaderDocumentation.requestHeaders(l) }),
            SnippetFactory(PathParameterDescriptor::class, { l -> RequestDocumentation.pathParameters(l) }),
            SnippetFactory(RequestParameterDescriptor::class, { l -> RequestDocumentation.requestParameters(l) })
    )

    private val responseSnippetMapping = mutableListOf(
            SnippetFactory(FieldDescriptor::class, { l -> PayloadDocumentation.responseFields(l) }),
            SnippetFactory(HeaderDescriptor::class, { l -> HeaderDocumentation.responseHeaders(l) }),
            SnippetFactory(LinkDescriptor::class, { l -> HypermediaDocumentation.links(l) })
    )

    private fun toSnippets(documentationContainer: DocumentationContainer): List<Snippet> {
        return descriptorsToSnippets(documentationContainer.request.descriptors, requestSnippetMapping)
                .plus(descriptorsToSnippets(documentationContainer.response.descriptors, responseSnippetMapping))
    }

    private fun descriptorsToSnippets(descriptors: List<AbstractDescriptor<*>>, mappings: List<SnippetFactory<out AbstractDescriptor<*>>>) =
            descriptors
                    .groupBy { it.javaClass }
                    .map { (key, value) ->
                        mappings
                                .first { key.isAssignableFrom(it.descriptorClass.java) }
                                .let { it as SnippetFactory<AbstractDescriptor<*>> }
                                .snippetCreator(value)
                    }
}

    class DocumentationContainer {
        internal var request = DocumentationRequest()
        internal var response = DocumentationResponse()

        fun request(requestProvider: DocumentationRequest.() -> Unit) {
            request = DocumentationRequest().apply(requestProvider)
        }

        fun response(responseProvider: DocumentationResponse.() -> Unit) {
            response = DocumentationResponse().apply(responseProvider)
        }
    }

    abstract class DocumentationOperation {

        internal val descriptors: MutableList<AbstractDescriptor<*>> = mutableListOf()

        fun field(path: String, descriptorFunction: FieldDescriptor.() -> Unit) {
            descriptors.add(fieldWithPath(path).apply(descriptorFunction))
        }

        fun header(name: String, descriptorFunction: HeaderDescriptor.() -> Unit) {
            descriptors.add(headerWithName(name).apply(descriptorFunction))
        }
    }

    class DocumentationRequest: DocumentationOperation() {
        fun pathParameter(name: String, descriptorFunction: ParameterDescriptor.() -> Unit) {
            descriptors.add(PathParameterDescriptor(name).apply(descriptorFunction))
        }

        fun requestParameter(name: String, descriptorFunction: ParameterDescriptor.() -> Unit) {
            descriptors.add(RequestParameterDescriptor(name).apply(descriptorFunction))
        }
    }

    class DocumentationResponse: DocumentationOperation() {
        fun link(rel: String, descriptorFunction: LinkDescriptor.() -> Unit) {
            descriptors.add(HypermediaDocumentation.linkWithRel(rel).apply(descriptorFunction))
        }
    }

    private class PathParameterDescriptor(name: String): ParameterDescriptor(name)
    private class RequestParameterDescriptor(name: String): ParameterDescriptor(name)
    private data class SnippetFactory<T: AbstractDescriptor<*>>(val descriptorClass: KClass<T>, val snippetCreator: (descriptors: List<T>) -> Snippet)
}
