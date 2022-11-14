package application.query

import common.{Page, PageQuery}
import domain.auth.repository.{PermissionRepository, RoleRepository}
import domain.user.value_obj.User
import interfaces.dto.RoleDto
import play.api.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AuthQueryService @Inject() (
  private val roleRepository: RoleRepository,
  private val permissionRepository: PermissionRepository
) extends Logging {

  def listRolesByPage(pageQuery: PageQuery): Future[Page[RoleDto]] = roleRepository.listByPage(pageQuery).map(_.map(RoleDto.formDo))

  def richUser(user: User): Future[User] =
    for {
      role        <- roleRepository.findByUserId(user.id)
      permissions <- if (role.isEmpty) Future.successful(Nil) else permissionRepository.findByRoleId(role.get.id)
    } yield user.copy(role = role, permissions = permissions)

}