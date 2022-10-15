package api.endpoints

import sttp.model.{Part, StatusCode}
import sttp.tapir._
//can not delete
import sttp.tapir.generic.auto._

object FileEndpoints {
  private val baseEndpoint = securedWithBearerEndpoint.in("files").tag("File API")

  final case class MultipartInput(file: Part[TapirFile]) extends Serializable

  val uploadEndpoint = baseEndpoint.post
    .name("fileUpload")
    .summary("上传文件")
    .description("上传文件接口")
    .in(multipartBody[MultipartInput])
    .in("upload")
    .out(statusCode(StatusCode.Ok))
    .out(stringBody)
}
