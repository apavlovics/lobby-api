package lv.continuum.lobby.model

/** A wrapper for JSON parsing errors. */
case class ParsingError(value: String) extends AnyVal {
  override def toString: String = value
}

object ParsingError {

  def apply(throwable: Throwable): ParsingError =
    ParsingError(s"JSON parsing error due to $throwable")
}
