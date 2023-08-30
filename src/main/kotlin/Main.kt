import kotlinx.serialization.json.*
import java.awt.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.http.*
import java.nio.file.Files
import java.time.LocalDate
import javax.imageio.ImageIO

const val inst_user_id = "17841460507708499"
const val inst_access_token = "EAAJUFwh4Qu8BO4qzBv7WMnGiOxrqZAnAVyl9yJ1H1rTPPjvGfzxwsNRpAzaKj7dc5ZAMcgbJaGwoqqO8tB2RoXXf0DYWyM7KaKYAJvcs6UEM23ANp7XExVtAgZAYPU3VzBIEMQTDjDYxNtH0eJkD6EKuovcPSamXCtA4zdTxkYlzDdXL8fWNYBGMNkd0gRMoOSFFw00BH8yqohHgJQC6AaxMwZDZD"
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
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    val font = Font.createFont(Font.TRUETYPE_FONT, File("C:\\Users\\HUAWEI\\Downloads\\Roboto\\Roboto-Regular.ttf"))
//    val font = Font.createFont(Font.TRUETYPE_FONT, File("C:\\Users\\HUAWEI\\Downloads\\Black_Ops_One\\BlackOpsOne-Regular.ttf"))
//    val font = Font.createFont(Font.TRUETYPE_FONT, File("C:\\Users\\HUAWEI\\Downloads\\Phudu\\Phudu-VariableFont_wght.ttf"))

    val color = Color.WHITE
    val shadow_color:Color? = color.darker().darker().darker()
    val dark_shadow_color:Color? = Color.BLACK

    val font_size = 42
    val font_style = Font.BOLD
    val custom_font = font.deriveFont(font_style, font_size.toFloat())
    graphics.font = custom_font
    graphics.color = dark_shadow_color
    val fontMetrics = graphics.fontMetrics
    val textWidth = fontMetrics.stringWidth(text)
    val x = (width - textWidth) / 2
    graphics.drawString(text, x + 2, height - 68)
    graphics.color = color
    graphics.drawString(text, x, height - 70)

    ImageIO.write(image, "jpg", image_file)
    graphics.dispose()
}

fun main() {

    val currentDate = LocalDate.now()
    val year = currentDate.year
    val month = currentDate.monthValue
    val day = currentDate.dayOfMonth

    val createdAtMin = "$year-$month-${day}T00:00:00-00:00"
    val shopify_uri = URI("https://eat-shop-sleep-and-repeat.myshopify.com/admin/products.json?created_at_min=$createdAtMin")

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
//    println(json_shopify_data.keys)
//    println(shopify_response.body().toString())

    val sep = System.getProperty("file.separator")
    val root = System.getProperty("user.dir")
    var i = 1
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

        val image_url = "https://shop-and-swap-pics.s3.eu-west-2.amazonaws.com/image$i.jpeg"
        val caption = ""
        val inst_response_to_create_media =  instagram_request_to_post(URI("https://graph.facebook.com/v17.0/$inst_user_id/media?access_token=$inst_access_token&caption=$caption&image_url=$image_url&media_type=STORIES"))
        val json_inst_data: Map<String, JsonElement> = Json.parseToJsonElement(inst_response_to_create_media.body()).jsonObject
        val media_id =  json_inst_data["id"].toString()
        val inst_response_to_post_media = instagram_request_to_post(URI("https://graph.facebook.com/v17.0/$inst_user_id/media_publish?creation_id=${media_id.substring(1, media_id.length - 1)}&access_token=$inst_access_token"))
        i += 1
    }
}