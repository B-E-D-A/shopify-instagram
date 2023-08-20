import kotlinx.serialization.json.*
import java.awt.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.http.*
import java.nio.file.Files
import javax.imageio.ImageIO

const val inst_user_id = "17841460507708499"
const val inst_access_token = "EAAJUFwh4Qu8BO8CRWhOhqeZAackEFon2MselCNCqhIG9lcCrPOcT4976ZCcBZBTPAqM7gLYrm4ZA2Yu1283zx1sWF5VZCBQqAWKPRX5NSveCuy1uyKMG57f34vZBNZAKhBZCrcqdLTgoYCrVgvvRGmlBzZA6nKyXAbzAr007KkeUKygS7AU1dThYaYwA2ZBzvXQbIKRA2FmjwDdqelRZAacF9ZAl5rXR"
const val shopify_access_token = "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"

val client = HttpClient.newBuilder().build()

fun download_image(image_url_string: String, file_path: String): File {
    val image_url  = URL(image_url_string.substring(1, image_url_string.length-1))
    val file = File(file_path)
    val image_data = image_url.readBytes()
    file.writeBytes(image_data)
    return file
}

fun instagram_request_to_post(uri: URI): HttpResponse<String>{
    val inst_request = HttpRequest.newBuilder()
        .uri(uri)
        .POST(HttpRequest.BodyPublishers.noBody())
        .build()
    val inst_response = client.send(inst_request, HttpResponse.BodyHandlers.ofString())
    if (inst_response.statusCode() != HttpURLConnection.HTTP_OK) {
        throw Exception("fail : ${inst_response.body()}")
    }
    return inst_response
}

fun edit_image_with_text(image_file: File, text: String) {
        val image = ImageIO.read(image_file)

        val width = image.width
        val height = image.height
        val graphics = image.createGraphics()

        val font = Font("San Serif.plain", Font.BOLD, 28)
/*
    for (f in GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts) {
        println("Font: ${f.name}; family: ${f.family}")
    }

 */
        val color = Color.WHITE
        val shadow_color:Color? = color.darker().darker().darker()
        graphics.font = font
        graphics.color = shadow_color
        graphics.drawString(text, width/2 - 100, height- 68)
        graphics.color = color
        graphics.drawString(text, width/2 - 102, height - 70)
        ImageIO.write(image, "jpg", image_file)
}

fun main() {

    val shopify_uri = URI("https://eat-shop-sleep-and-repeat.myshopify.com/admin/products.json")

    val shopify_request = HttpRequest.newBuilder()
        .uri(shopify_uri)
        .header("X-Shopify-Access-Token", shopify_access_token)
        .GET()
        .build()

    val shopify_response = client.send(shopify_request, HttpResponse.BodyHandlers.ofString())
    if(shopify_response.statusCode() != HttpURLConnection.HTTP_OK){
        throw Exception("fail : ${shopify_response.body()}")
    }
    val json_shopify_data: Map<String, JsonElement> = Json.parseToJsonElement(shopify_response.body()).jsonObject

    val sep = System.getProperty("file.separator")
    val root = System.getProperty("user.dir")
    val i = 1
    for(item in json_shopify_data["products"]!!.jsonArray) {
        val item_image = download_image(item.jsonObject["image"]!!.jsonObject["src"].toString(), "$root${sep}image$i.jpg")
        val item_title = item.jsonObject["title"].toString()
        edit_image_with_text(item_image, item_title.substring(1,item_title.length-1))

        val s3_bucket_name="shop-and-swap-pics"
        val item_image_data = Files.readAllBytes(item_image.toPath())
        val s3_request_to_upload = HttpRequest.newBuilder()
            .uri(URI("https://ka3xs73p6a.execute-api.eu-west-2.amazonaws.com/dev2/$s3_bucket_name/image$i.jpeg"))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(item_image_data))
            .header("Content-Type", "image/jpeg")
            .build()

        val s3_response_to_upload = client.send(s3_request_to_upload, HttpResponse.BodyHandlers.ofString())
        if(s3_response_to_upload.statusCode() != HttpURLConnection.HTTP_OK){
            throw Exception("fail : ${s3_response_to_upload.body()}")
        }
        else{
            println(s3_response_to_upload.body().toString())
            println("OK1")
        }

        val image_url = "https://shop-and-swap-pics.s3.eu-west-2.amazonaws.com/image$i.jpeg"
//        println(image_url)
        val caption = ""
        val inst_response_to_create_media =  instagram_request_to_post(URI("https://graph.facebook.com/v17.0/$inst_user_id/media?access_token=$inst_access_token&caption=$caption&image_url=$image_url"))
        val json_inst_data: Map<String, JsonElement> = Json.parseToJsonElement(inst_response_to_create_media.body()).jsonObject
        val media_id =  json_inst_data["id"].toString()
        val inst_response_to_post_media = instagram_request_to_post(URI("https://graph.facebook.com/v17.0/$inst_user_id/media_publish?creation_id=${media_id.substring(1, media_id.length - 1)}&access_token=$inst_access_token"))
        println(inst_response_to_post_media.body())

    }
}