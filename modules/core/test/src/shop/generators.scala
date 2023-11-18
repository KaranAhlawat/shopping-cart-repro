package shop

import shop.domain.auth._
import shop.domain.brand.{Brand, BrandId, BrandName}
import shop.domain.cart.*
import shop.domain.category.{Category, CategoryId, CategoryName}
import shop.domain.checkout.*
import shop.domain.item.*
import shop.domain.order.{OrderId, PaymentId}
import shop.domain.payment.Payment
import shop.http.auth.auth.{CommonUser, User}

import org.scalacheck.Gen
import squants.market.{INR, Money}

import java.util.UUID

object generators:
  val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap(n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      )

  def nesGen[A](f: String => A): Gen[A] =
    nonEmptyStringGen.map(f)

  def idGen[A](f: UUID => A): Gen[A] =
    Gen.uuid.map(f)

  val brandIdGen: Gen[BrandId] =
    idGen(BrandId.applyUnsafe(_))

  val brandNameGen: Gen[BrandName] =
    nesGen(BrandName.applyUnsafe(_))

  val categoryIdGen: Gen[CategoryId] =
    idGen(CategoryId.applyUnsafe(_))

  val categoryNameGen: Gen[CategoryName] =
    nesGen(CategoryName.applyUnsafe(_))

  val itemIdGen: Gen[ItemId] =
    idGen(ItemId.applyUnsafe(_))

  val itemNameGen: Gen[ItemName] =
    nesGen(ItemName.applyUnsafe(_))

  val itemDescriptionGen: Gen[ItemDescription] =
    nesGen(ItemDescription.applyUnsafe(_))

  val orderIdGen: Gen[OrderId] =
    idGen(OrderId.applyUnsafe(_))

  val userIdGen: Gen[UserId] =
    idGen(UserId.applyUnsafe(_))

  val paymentIdGen: Gen[PaymentId] =
    idGen(PaymentId.applyUnsafe(_))

  val userNameGen: Gen[UserName] =
    nesGen(UserName.applyUnsafe(_))

  val brandGen: Gen[Brand] =
    for
      i <- brandIdGen
      n <- brandNameGen
    yield Brand(i, n)

  val categoryGen: Gen[Category] =
    for
      i <- categoryIdGen
      n <- categoryNameGen
    yield Category(i, n)

  val moneyGen: Gen[Money] =
    Gen.posNum[Long].map(n =>
      INR(BigDecimal(n))
    )

  val itemGen: Gen[Item] =
    for
      i <- itemIdGen
      n <- itemNameGen
      d <- itemDescriptionGen
      p <- moneyGen
      b <- brandGen
      c <- categoryGen
    yield Item(i, n, d, p, b, c)

  val quantityGen: Gen[Quantity] =
    Gen.posNum[Int].map(Quantity.applyUnsafe(_))

  val cartItemGen: Gen[CartItem] =
    for
      i <- itemGen
      q <- quantityGen
    yield CartItem(i, q)

  val cartTotalGen: Gen[CartTotal] =
    for
      i <- Gen.nonEmptyListOf(cartItemGen)
      t <- moneyGen
    yield CartTotal(i, t)

  val itemMapGen: Gen[(ItemId, Quantity)] =
    for
      i <- itemIdGen
      q <- quantityGen
    yield i -> q

  val cartGen: Gen[Cart] =
    Gen.nonEmptyMap(itemMapGen).map(Cart.applyUnsafe(_))

  val cardNameNesGen: Gen[CardName] =
    nesGen(CardName.applyUnsafe(_))

  private def sized(size: Int): Gen[Long] =
    def go(s: Int, acc: String): Gen[Long] =
      Gen.oneOf(1 to 9).flatMap(n =>
        if s == size then acc.toLong
        else go(s + 1, acc + n.toString)
      )

    go(0, "")

  val cardGen: Gen[Card] =
    for
      n <- cardNameNesGen
      u <- sized(16).map(x => CardNumber.applyUnsafe(x))
      x <- sized(4).map(x => CardExpiration.applyUnsafe(x.toString))
      c <- sized(3).map(x => CardCVV.applyUnsafe(x.toInt))
    yield Card(n, u, x, c)

  val userGen: Gen[User] =
    for
      i <- userIdGen
      n <- userNameGen
    yield User(i, n)

  val commonUserGen: Gen[CommonUser] =
    userGen.map(CommonUser.applyUnsafe(_))

  val paymentGen: Gen[Payment] =
    for
      i <- userIdGen
      m <- moneyGen
      c <- cardGen
    yield Payment(i, m, c)

  val encryptedPasswordGen: Gen[EncryptedPassword] =
    nesGen(EncryptedPassword.applyUnsafe(_))

  val passwordGen: Gen[Password] =
    nesGen(Password.applyUnsafe(_))
