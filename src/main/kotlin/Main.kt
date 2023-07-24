import java.net.URI
import java.net.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject


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

    val json: Map<String, JsonElement> = Json.parseToJsonElement(response_body).jsonObject
    println(json["products"]!!.jsonArray[0].jsonObject.keys)
}