package infra.db.repository

import common.{Page, PageQuery}
import infra.db.po.{CategoryPo, TagPo}

import scala.concurrent.Future

trait QueryRepository[T] {

  def get(id: Long): Future[Option[T]]

  def list(): Future[Seq[T]]

  def count(): Future[Int]

  def listByPage(pageQuery: PageQuery): Future[Page[T]]

  def listTagsById(tagIds: Seq[Long]): Future[Seq[TagPo]]

  def getCategoryById(categoryId: Long): Future[Option[CategoryPo]]

}
