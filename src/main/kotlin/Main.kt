import java.net.URI
import java.net.URL
import java.net.http.*
import kotlinx.serialization.json.*
import java.io.File
import kotlin.io.readBytes

fun download_image(image_url_string: String, file_path: String): File {
    val image_url  = URL(image_url_string.substring(1, image_url_string.length-1))
    val file = File(file_path)
    val image_data = image_url.readBytes()
    file.writeBytes(image_data)
    return file
}
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
    val response_body : String = if (response_status==200) response.body() else "error $response_status"

    val json_data: Map<String, JsonElement> = Json.parseToJsonElement(response_body).jsonObject
//    println(json_data["products"]!!.jsonArray[0].jsonObject.keys)

    val sep = System.getProperty("file.separator")
    val root = System.getProperty("user.dir")
    for(item in json_data["products"]!!.jsonArray){
        val item_image = download_image(item.jsonObject["image"]!!.jsonObject["src"].toString(), "$root${sep}imagetest.jpg")
        val item_title = item.jsonObject["title"]
//        println(item_title)
    }

    //Instagram API
    val user_id = "17841460507708499"
    val inst_access_token = "EAAJUFwh4Qu8BAMdWl7gnPZBbO5PgNlDKs4xP1wEp1lWGdSg3TKcBzbV5G7zLaSzaPInkRRyEArJKY71YNZBJUyft7QT96vTWx5sdxNZBOjSWVqKatJ1agNNNGnaaccJXkQqDH3aHjMZBIPnLtgK5SdgyG9GHrfI06TaZAz7XHikTkfdqMGeyGCGhKZCA3mLyEOZBPwF9VZBYE1xEo1NfyYC1Cnovm2nhqrgZD"

    val inst_client = HttpClient.newBuilder().build()
    val inst_uri = URI.create("https://graph.instagram.com/$user_id?fields=id,username,full_name&access_token=$inst_access_token")
    val inst_uri_2 = URI.create("https://api.instagram.com/v1/users/$user_id/?access_token=$inst_access_token")
    val inst_request = HttpRequest.newBuilder()
        .uri(inst_uri_2)
        .GET()
        .build()
//    val inst_response = inst_client.send(inst_request, HttpResponse.BodyHandlers.ofString())
}