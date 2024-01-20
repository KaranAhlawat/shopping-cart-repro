package shop.sql

import shop.domain.auth.{EncryptedPassword, UserId, UserName}
import shop.domain.brand.*
import shop.domain.category.*
import shop.domain.item.*
import shop.domain.order.{OrderId, PaymentId}

import io.github.iltotore.iron.*
import skunk.*
import skunk.codec.all.*
import squants.market.{INR, Money}

object codecs:
  val brandId: Codec[BrandId] = uuid.imap[BrandId](BrandId(_))(_.value)
  val brandName: Codec[BrandName] = varchar.imap[BrandName](BrandName(_))(_.value)

  val categoryId: Codec[CategoryId] = uuid.imap(CategoryId(_))(_.value)
  val categoryName: Codec[CategoryName] = varchar.imap[CategoryName](CategoryName(_))(_.value)

  val itemId: Codec[ItemId] = uuid.imap(ItemId(_))(_.value)
  val itemName: Codec[ItemName] = varchar.imap(ItemName(_))(_.value)
  val itemDesc: Codec[ItemDescription] = varchar.imap(ItemDescription(_))(_.value)

  val orderId: Codec[OrderId] = uuid.imap(OrderId(_))(_.value)
  val paymentId: Codec[PaymentId] = uuid.imap(PaymentId(_))(_.value)

  val userId: Codec[UserId] = uuid.imap(UserId(_))(_.value)
  val userName: Codec[UserName] = varchar.imap(UserName(_))(_.value)

  val money: Codec[Money] = numeric.imap(INR(_))(_.amount)
  val encPassword: Codec[EncryptedPassword] = varchar.imap(EncryptedPassword(_))(_.value)
