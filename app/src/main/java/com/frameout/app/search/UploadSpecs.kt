package com.frameout.app.search

import com.frameout.core.search.ImageSearchEngines
import com.frameout.core.search.SearchEngine

internal data class UploadSpec(
    val origin: String,
    val action: String,
    val field: String,
    val base64: Boolean = false,
    val jsonResult: Boolean = false
) {
    fun html(): String {
        if (jsonResult) return jsonHtml()
        val value = if (base64) {
            "var r=new FileReader();r.onload=function(){t.value=String(r.result).split(',')[1];f.submit();};r.readAsDataURL(b);"
        } else {
            "var d=new DataTransfer();d.items.add(new File([b],'frame.jpg',{type:b.type||'image/jpeg'}));t.files=d.files;f.submit();"
        }
        val input = if (base64) "<input type=\"hidden\" name=\"$field\" id=\"t\">" else "<input type=\"file\" name=\"$field\" id=\"t\">"
        return "<!doctype html><meta name=\"viewport\" content=\"width=device-width\"><body style=\"background:#000;color:#ccc;font-family:sans-serif\">" +
            "<form id=\"f\" method=\"post\" enctype=\"multipart/form-data\" action=\"$action\">$input</form><p>…</p>" +
            "<script>var f=document.getElementById('t').form,t=document.getElementById('t');" +
            "fetch('${ImageSearchActivity.IMAGE_PATH}').then(function(r){return r.blob()}).then(function(b){$value}).catch(function(){document.body.textContent='upload failed'});</script>"
    }
}

private const val SHELL_HEAD = "<!doctype html><meta name=\"viewport\" content=\"width=device-width\"><body style=\"background:#000;color:#ccc;font-family:sans-serif\"><p>…</p>"

private fun UploadSpec.jsonHtml(): String {
    val request = "{\"blocks\":[{\"block\":\"b-page_type_search-by-image__link\"}]}"
    val url = "$action&format=json&request=" + java.net.URLEncoder.encode(request, "UTF-8")
    return SHELL_HEAD + "<script>fetch('${ImageSearchActivity.IMAGE_PATH}').then(function(r){return r.blob()}).then(function(b){" +
        "var d=new FormData();d.append('$field',b,'blob');" +
        "return fetch('$url',{method:'POST',body:d,credentials:'include'})})" +
        ".then(function(r){return r.json()}).then(function(j){location.href='$origin/images/search?'+j.blocks[0].params.url})" +
        ".catch(function(){location.href='$origin/images/'});</script>"
}

internal object UploadSpecs {
    fun of(engine: SearchEngine): UploadSpec? = when (engine.id) {
        ImageSearchEngines.LENS_ID -> UploadSpec("https://lens.google.com", "https://lens.google.com/v3/upload?ep=gsbubb", "encoded_image")
        "google_images" -> UploadSpec("https://www.google.com", "https://www.google.com/searchbyimage/upload", "encoded_image")
        "yandex_images" -> UploadSpec("https://yandex.com", "https://yandex.com/images/search?rpt=imageview", "upfile", jsonResult = true)
        "bing_visual" -> UploadSpec("https://www.bing.com", "https://www.bing.com/images/search?view=detailv2&iss=sbiupload&FORM=SBIIRP", "imageBin", base64 = true)
        "tineye" -> UploadSpec("https://tineye.com", "https://tineye.com/search", "image")
        "saucenao" -> UploadSpec("https://saucenao.com", "https://saucenao.com/search.php", "file")
        else -> null
    }
}
