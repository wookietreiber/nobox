package nobox

import sbt._
import nobox.Type._

object Generate{

  val list = List(BOOL, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE)

  def apply(dir: File): Seq[File] = {
    list.map{ t =>
      val f = dir / ("of" + t + ".scala")
      IO.write(f, src(t))
      f
    }
  }

  def src(a: Type): String = {

    val clazz = "of" + a

    val map: String => String = { b =>
s"""
  def map$b(f: $a => $b): of$b = {
    val array = new Array[$b](self.length)
    var i = 0
    while(i < self.length){
      array(i) = f(self(i))
      i += 1
    }
    new of$b(array)
  }
"""
    }

    val reverseMap: String => String = { b =>
s"""
  def reverseMap$b(f: $a => $b): of$b = {
    val len = self.length
    val array = new Array[$b](len)
    var i = 0
    while(i < len){
      array(len - i - 1) = f(self(i))
      i += 1
    }
    new of$b(array)
  }
"""
    }

    val flatMap: String => String = { b =>
s"""
  def flatMap$b(f: $a => Array[$b]): of$b = {
    val builder = new ArrayBuilder.of$b()
    var i = 0
    while(i < self.length){
      val x = f(self(i))
      var j = 0
      while(j < x.length){
        builder += x(j)
        j += 1
      }
      i += 1
    }
    new of$b(builder.result)
  }
"""
    }

    val collect: String => String = { b =>
s"""
  def collect$b(f: PartialFunction[$a, $b]): of$b = {
    val builder = new ArrayBuilder.of$b()
    var i = 0
    while(i < self.length){
      if(f isDefinedAt self(i)){
        builder += f(self(i))
      }
      i += 1
    }
    new of$b(builder.result)
  }
"""
    }

    val collectFirst: String => String = { b =>
s"""
  def collectFirst$b(f: PartialFunction[$a, $b]): Option[$b] = {
    var i = 0
    while(i < self.length){
      if(f isDefinedAt self(i)){
        return Some(f(self(i)))
      }
      i += 1
    }
    None
  }
"""
    }

    // could not use @specialized annotation with value class
    // https://gist.github.com/xuwei-k/7153650
    val foldLeft: String => String = { b =>
s"""
  def foldLeft$b(z: $b)(f: ($b, $a) => $b): $b = {
    var i = 0
    var acc = z
    while(i < self.length){
      acc = f(acc, self(i))
      i += 1
    }
    acc
  }
"""
    }

    val foldRight: String => String = { b =>
s"""
  def foldRight$b(z: $b)(f: ($a, $b) => $b): $b = {
    var i = self.length - 1
    var acc = z
    while(i >= 0){
      acc = f(self(i), acc)
      i -= 1
    }
    acc
  }
"""
    }

    val sum: String = a match {
      case BYTE | CHAR | SHORT | INT =>
s"""
  def sum: Int = {
    var i, n = 0
    while(i < self.length){
      n += self(i)
      i += 1
    }
    n
  }
"""
      case BOOL => ""
      case DOUBLE | FLOAT | LONG =>
s"""
  def sum: $a = {
    var i = 0
    var n: $a = 0
    while(i < self.length){
      n += self(i)
      i += 1
    }
    n
  }
"""
    }

    val sumLong: String = a match {
      case INT =>
s"""
  def sumLong: Long = {
    var i = 0
    var n: Long = 0L
    while(i < self.length){
      n += self(i)
      i += 1
    }
    n
  }
"""
      case _ => ""
    }

    val product: String = a match {
      case BYTE | CHAR | SHORT | INT =>
s"""
  def product: Int = {
    var i = 0
    var n = 1
    while(i < self.length){
      n *= self(i)
      i += 1
    }
    n
  }
"""
      case BOOL => ""
      case DOUBLE | FLOAT | LONG =>
s"""
  def product: $a = {
    var i = 0
    var n: $a = 1
    while(i < self.length){
      n *= self(i)
      i += 1
    }
    n
  }
"""
    }

    val productLong: String = a match {
      case BYTE | CHAR | SHORT | INT =>
s"""
  def productLong: Long = {
    var i = 0
    var n: Long = 1L
    while(i < self.length){
      n *= self(i)
      i += 1
    }
    n
  }
"""
      case _ => ""
    }

    val productDouble: String = a match {
      case BYTE | CHAR | SHORT | INT | LONG | FLOAT =>
s"""
  def productDouble: Double = {
    var i = 0
    var n: Double = 1.0
    while(i < self.length){
      n *= self(i)
      i += 1
    }
    n
  }
"""
      case _ => ""
    }

    val sorted: String = a match {
      case BOOL => ""
      case _ =>
s"""
  def sorted: $clazz = {
    val array = self.clone
    Arrays.sort(array)
    new $clazz(array)
  }
"""
    }

    val methods: String = List(
      map, reverseMap, flatMap, collect, collectFirst, foldLeft, foldRight
    ).map{ method =>
      list.map(_.toString) map method mkString "\n"
    }.mkString("\n\n")

s"""package nobox

import java.util.Arrays
import scala.collection.mutable.ArrayBuilder

final class $clazz (val self: Array[$a]) extends AnyVal {
  $methods

  $sum

  $sumLong

  $product

  $productLong

  $productDouble

  $sorted

  def filter(f: $a => Boolean): $clazz = {
    val builder = new ArrayBuilder.of$a()
    var i = 0
    while(i < self.length){
      if(f(self(i))){
        builder += self(i)
      }
      i += 1
    }
    new $clazz(builder.result)
  }

  def filterNot(f: $a => Boolean): $clazz = filter(!f(_))

  def withFilter(f: $a => Boolean): $clazz = filter(f)

  def find(f: $a => Boolean): Option[$a] = {
    var i = 0
    while(i < self.length){
      if(f(self(i))){
        return Some(self(i))
      }
      i += 1
    }
    None
  }

  def exists(f: $a => Boolean): Boolean = {
    var i = 0
    while(i < self.length){
      if(f(self(i))){
        return true
      }
      i += 1
    }
    false
  }

  def forall(f: $a => Boolean): Boolean = !exists(!f(_))

  def take(n: Int): $clazz = {
    val len = (self.length min n) max 0
    new $clazz(Arrays.copyOf(self, len))
  }

  def takeWhile(f: $a => Boolean): $clazz = {
    val len = index(!f(_))
    if(len < 0){
      this
    }else if(len == 0){
      $clazz.empty
    }else{
      new $clazz(Arrays.copyOf(self, len))
    }
  }

  def takeRight(n: Int): $clazz = {
    if(n <= 0){
      $clazz.empty
    }else if(n >= self.length){
      this
    }else{
      val start = self.length - n
      new $clazz(Arrays.copyOfRange(self, start, self.length))
    }
  }

  def reverse: $clazz = {
    var i = 0
    val len = self.length
    val array = new Array[$a](len)
    while(i < len){
      array(len - i - 1) = self(i)
      i += 1
    }
    new $clazz(array)
  }

  def reverse_:::(prefix: $clazz): $clazz = {
    if(prefix.length == 0){
      this
    }else{
      val array = new Array[$a](self.length + prefix.length)
      var i = 0
      val len = prefix.length
      while(i < len){
        array(i) = prefix.self(len - i - 1)
        i += 1
      }
      System.arraycopy(self, 0, array, prefix.length, self.length)
      new $clazz(array)
    }
  }

  def count(f: $a => Boolean): Int = {
    var i = 0
    var n = 0
    while(i < self.length){
      if(f(self(i))){
        n += 1
      }
      i += 1
    }
    n
  }

  def drop(n: Int): $clazz = {
    if(n <= 0){
      this
    }else if(n >= self.length){
      $clazz.empty
    }else{
      new $clazz(Arrays.copyOfRange(self, n, self.length))
    }
  }

  def dropWhile(f: $a => Boolean): $clazz = {
    val len = index(!f(_))
    if(len < 0){
      $clazz.empty
    }else if(len == 0){
      this
    }else{
      new $clazz(Arrays.copyOfRange(self, len, self.length))
    }
  }

  def dropRight(n: Int): $clazz = {
    if(n <= 0){
      this
    }else if(n >= self.length){
      $clazz.empty
    }else{
      new $clazz(Arrays.copyOf(self, self.length - n))
    }
  }

  def contains(elem: $a): Boolean = {
    var i = 0
    while(i < self.length){
      if(self(i) == elem){
        return true
      }
      i += 1
    }
    false
  }

  def splitAt(n: Int): ($clazz, $clazz) = {
    if(n <= 0){
      ($clazz.empty, this)
    }else if(n >= self.length){
      (this, $clazz.empty)
    }else{
      (new $clazz(Arrays.copyOf(self, n)), new $clazz(Arrays.copyOfRange(self, n, self.length)))
    }
  }

  def span(f: $a => Boolean): ($clazz, $clazz) = {
    val n = index(!f(_))
    if(n < 0){
      (this, $clazz.empty)
    }else if(n >= self.length){
      ($clazz.empty, this)
    }else{
      (new $clazz(Arrays.copyOf(self, n)), new $clazz(Arrays.copyOfRange(self, n, self.length)))
    }
  }

  def ++(that: $clazz): $clazz = {
    if(self.length == 0){
      that
    }else if(that.length == 0){
      this
    }else{
      val size1 = self.length
      val size2 = that.length
      val array = new Array[$a](size1 + size2)
      System.arraycopy(self, 0, array, 0, size1)
      System.arraycopy(that.self, 0, array, size1, size2)
      new $clazz(array)
    }
  }

  def partition(f: $a => Boolean): ($clazz, $clazz) = {
    val l, r = new ArrayBuilder.of$a()
    var i = 0
    while(i < self.length){
      if(f(self(i))){
        l += self(i)
      }else{
        r += self(i)
      }
      i += 1
    }
    (new $clazz(l.result), new $clazz(r.result))
  }

  def updated(index: Int, elem: $a): $clazz = {
    val array = self.clone
    array(index) = elem
    new $clazz(array)
  }

  def slice(from: Int, until: Int): $clazz = {
    if(until <= from || until <= 0 || from >= self.length){
      $clazz.empty
    }else if(from <= 0 && self.length <= until){
      this
    }else{
      new $clazz(Arrays.copyOfRange(self, from max 0, until min self.length))
    }
  }

  def reduceLeftOption(f: ($a, $a) => $a): Option[$a] = {
    if(self.length == 0) return None

    var i = 1
    var acc = self(0)
    while(i < self.length){
      acc = f(acc, self(i))
      i += 1
    }
    Some(acc)
  }

  def reduceRightOption(f: ($a, $a) => $a): Option[$a] = {
    if(self.length == 0) return None

    var i = self.length - 2
    var acc = self(self.length - 1)
    while(i >= 0){
      acc = f(self(i), acc)
      i -= 1
    }
    Some(acc)
  }

  def foldLeft[A](z: A)(f: (A, $a) => A): A = {
    var i = 0
    var acc = z
    while(i < self.length){
      acc = f(acc, self(i))
      i += 1
    }
    acc
  }

  def foldRight[A](z: A)(f: ($a, A) => A): A = {
    var i = self.length - 1
    var acc = z
    while(i >= 0){
      acc = f(self(i), acc)
      i -= 1
    }
    acc
  }

  def length: Int = self.length

  def size: Int = self.length

  @inline private def index(f: $a => Boolean): Int = {
    var i = 0
    while(i < self.length){
      if(f(self(i))){
        return i
      }
      i += 1
    }
    -1
  }

  override def toString = self.mkString("$clazz(", ", ", ")")

  def ===(that: $clazz): Boolean = self sameElements that.self
}

object $clazz {

  def apply(elems: $a *): $clazz = new $clazz(elems.toArray)

  val empty: $clazz = new $clazz(new Array[$a](0))
}
"""
  }
}

