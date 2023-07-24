import java.net.URI
import java.net.http.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.annotation.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

data class Item @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("id") val id: Long,
    @JsonProperty("title") val title: String?,
    @JsonProperty("body_html") val body_html: String?,
    @JsonProperty("vendor") val vendor: String?,
    @JsonProperty("product_type") val product_type: String?,
    @JsonProperty("created_at") val created_at: String?,
    @JsonProperty("handle") val handle: String?,
    @JsonProperty("updated_at") val updated_at: String?,
    @JsonProperty("published_at") val published_at: String?,
    @JsonProperty("template_suffix") val template_suffix: String?,
    @JsonProperty("status") val status: String?,
    @JsonProperty("published_scope") val published_scope: String?,
    @JsonProperty("tags") val tags: String?,
    @JsonProperty("admin_graphql_api_id") val admin_graphql_api_id: String?,
//    @JsonIgnore val variants : String?
    @JsonProperty("variants") val variants: List<Item>?
//    @JsonProperty("price") val price: Int
)

data class Products @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("products") val products: List<Item>
)

val test_json =  """
    {
      "id": "12345",
      "title": "Important thing",
      "product_type": "things",
      "created_at": "beginning",
      "updated_at": "some time ago",
      "published_at": "now",
      "price": "1"
    }
"""

fun main() {
    val access_token = "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"
    val url = "https://eat-shop-sleep-and-repeat.myshopify.com/admin/products.json"

    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("X-Shopify-Access-Token", access_token)
        .GET()
        .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val response_status = response.statusCode();
    var response_body : String = if (response_status==200) response.body() else "error $response_status"
    println("Response body: $response_body")

    val mapper = ObjectMapper()

    val json: Map<String, JsonElement> = Json.parseToJsonElement(response_body).jsonObject
    println(json["products"]!!.jsonArray[0].jsonObject.keys)

//    mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

//    val items = mapper.readValue(response_body?.trimIndent(), Products::class.java)
//    for (item in items) {
//        println("ID: ${item.id},\n" +
//                "Title: ${item.title},\n" +
//                "Product Type: ${item.product_type},\n" +
//                "Created At: ${item.created_at},\n" +
//                "Updated At: ${item.updated_at},\n" +
//                "Published At: ${item.published_at},\n" +
//                "Price: ${item.price}")
//    }
}