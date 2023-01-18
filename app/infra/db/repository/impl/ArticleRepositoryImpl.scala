package infra.db.repository.impl

import common.Constant
import domain.action.Action
import domain.article.{Article, ArticleRepository}
import infra.db.assembler.ArticleAssembler._
import infra.db.po.ActionPo.ActionTable
import infra.db.po.ArticlePo.ArticleTable
import infra.db.po.CategoryPo.CategoryTable
import infra.db.po.TagPo.TagTable
import infra.db.repository.ArticleQueryRepository
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfig}
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ArticleRepositoryImpl @Inject() (private val dbConfigProvider: DatabaseConfigProvider, queryRepository: ArticleQueryRepository)(
  implicit ec: ExecutionContext
) extends ArticleRepository
    with HasDatabaseConfig[PostgresProfile] {

  override protected val dbConfig: DatabaseConfig[PostgresProfile] = dbConfigProvider.get[PostgresProfile]

  private val articles   = TableQuery[ArticleTable]
  private val tags       = TableQuery[TagTable]
  private val categories = TableQuery[CategoryTable]
  private val actions    = TableQuery[ActionTable]

  override def save(article: Article): Future[Long] =
    article.id match {
      case Constant.domainCreateId => doInsert(article)
      case _                       => doUpdate(article)
    }

  override def get(id: Long): Future[Option[Article]] =
    db.run(articles.filter(_.id === id).result.headOption) flatMap {
      case None => Future.successful(None)
      case Some(articlePo) =>
        val tagIds     = articlePo.tags.split(",").map(_.toLong)
        val categoryId = articlePo.category

        val selectTags = queryRepository.listTagsById(tagIds.toSeq)
        val selectCategory =
          if (categoryId.isEmpty) Future.successful(None)
          else queryRepository.getCategoryById(categoryId.get)
        val selectAction = db.run(actions.filter { a =>
          Seq(a.resourceId === articlePo.id, a.typ inSet Seq(Action.Type.LICK_ARTICLE, Action.Type.VIEW_ARTICLE)).reduce(_ && _)
        }.result)

        val future = for {
          tags     <- selectTags
          category <- selectCategory
          actions  <- selectAction
        } yield (tags, category, actions)

        future.map { tuple =>
          val actionMap = tuple._3.groupBy(_.typ)
          val viewCount = actionMap.get(Action.Type.VIEW_ARTICLE).size
          val likeCount = actionMap.get(Action.Type.LICK_ARTICLE).size
          Some(toDo(articlePo).copy(tags = tuple._1.map(toDo), category = tuple._2.map(toDo), viewCount = viewCount, likeCount = likeCount))
        }
    }

  override def remove(id: Long): Future[Unit] = db.run(articles.filter(_.id === id).delete).map(_ => ())

  private def doInsert(article: Article): Future[Long] = db.run(articles returning articles.map(_.id) += article)

  private def doUpdate(article: Article): Future[Long] = db.run(articles.filter(_.id === article.id).update(article)).map(_ => article.id)

}