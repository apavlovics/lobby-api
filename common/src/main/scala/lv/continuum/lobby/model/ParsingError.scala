package lv.continuum.lobby.model

/** A wrapper for JSON parsing errors. */
opaque type ParsingError = String
object ParsingError {

  def apply(throwable: Throwable): ParsingError =
    s"JSON parsing error due to $throwable"
}
