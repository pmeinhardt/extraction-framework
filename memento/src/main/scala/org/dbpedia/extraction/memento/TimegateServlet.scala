package org.dbpedia.extraction.memento

import java.text.SimpleDateFormat
import java.util.{TimeZone, Locale, Date}
import org.apache.log4j.Logger
import org.dbpedia.extraction.destinations.formatters.{UriPolicy, TerseFormatter}
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.timemachine.{RevisionHistory, TimeMachine}
import org.scalatra.ScalatraServlet

/**
 * Implements a Memento Timegate as specified in RFC 7089
 * http://tools.ietf.org/html/rfc7089
 */
class TimegateServlet extends ScalatraServlet {
  val policies  = UriPolicy.parsePolicy(LiveOptions.options.get("uri-policy.main"))
  val formatter = new TerseFormatter(false, true, policies)

  val timezone  = TimeZone.getTimeZone("GMT")

  val logger = Logger.getLogger(getClass)

  before() {
    logger.info("%s %s".format(request.getMethod, request.getPathInfo))
  }

  /**
   * Returns all triples for the requested resource, either as "original" or "memento".
   *
   * Original Resource acts as its own TimeGate and Memento, see section 4.1.3
   * http://tools.ietf.org/html/rfc7089#section-4.1.3
   *
   * In order to retrieve a Memento, clients may pass a timestamp
   * via the "Accept-Datetime" HTTP Header described in RFC 7089.
   *
   * When no such timestamp is passed, returns the Original Resource.
   *
   * You can request a resource using cURL, similar to the following commands:
   * $ curl -i --header "Accept-Datetime: Sun, 18 May 2014 01:23:00 GMT" http://localhost:8081/pages/5
   * $ curl -i http://localhost:8081/pages/5
   */
  get("/pages/:id") {
    val datetime = Option(request.getHeader("Accept-Datetime"))
    val id = params("id").toLong

    datetime match {
      case Some(str) => memento(id, datefmt.parse(str))
      case _ => original(id)
    }
  }

  /**
   * Returns a version of the requested resource at the given time.
   *
   * You can request a resource version using cURL as follows:
   * $ curl -i http://localhost:8081/pages/5/versions/20140518012300
   */
  get("/pages/:id/versions/:datetime") {
    val datetime = params("datetime")
    val id = params("id").toLong

    memento(id, shortfmt.parse(datetime))
  }

  /**
   * Returns a TimeMap for the specified resource.
   *
   * Supports "application/link" format.
   *
   * SiteMap content and serialization is specified in RFC 7089, section 5
   * http://tools.ietf.org/html/rfc7089#section-5
   *
   * An example request using cURL:
   * $ curl -i http://localhost:8081/pages/5/history
   */
  get("/pages/:id/history") {
    val id = params("id").toLong

    val history = new RevisionHistory(id)
    val res = new StringBuilder

    res.append("<%s>; rel=\"self\"; type=\"application/link-format\",\n".format(url))
    res.append("<%s://%s%s/pages/%d>; rel=\"original\"".format(scheme, host, base, id))

    // val timestamps = history.timestamps(0)
    val timestamps = List[Long](100000, 60000, 200, 0)

    val URLFmt = shortfmt
    val lnkFmt = datefmt

    for (ts <- timestamps) {
      val version = URLFmt.format(new Date(ts))
      res.append(",\n<%s://%s%s/pages/%d/versions/%s>; rel=\"memento\"".format(scheme, host, base, id, version))
      res.append("; datetime=\"%s\"".format(lnkFmt.format(ts)))
    }

    response.setHeader("Content-Type", "application/link-format")

    res.toString()
  }

  // Route helpers

  def original(id: Long): String = {
    val machine = new TimeMachine
    val triples = machine.current(id)

    response.setHeader("Vary", "accept-datetime")
    response.setHeader("Link", "<%s>; rel=\"timegate\", <%s>; rel=\"timemap\"".format(url, url + "/history"))

    render(triples)
  }

  def memento(id: Long, date: Date): String = {
    val machine = new TimeMachine
    val (triples, timestamp) = machine.retrieve(id, date.getTime)

    val orig = scheme + "://" + host + base + "/pages/" + id

    response.setHeader("Vary", "accept-datetime")
    response.setHeader("Memento-Datetime", datefmt.format(new Date(timestamp)))
    response.setHeader("Link", "<%s>; rel=\"original timegate\", <%s>; rel=\"timemap\"".format(orig, orig + "/history"))

    render(triples)
  }

  def render(triples: Set[Quad]): String = {
    triples.map((q) => formatter.render(q)).mkString
  }

  // Utils

  def datefmt: SimpleDateFormat = {
    val fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
    fmt.setTimeZone(timezone)
    fmt
  }

  def shortfmt: SimpleDateFormat = {
    val fmt = new SimpleDateFormat("yyyymmddHHmmss")
    fmt.setTimeZone(timezone)
    fmt
  }

  def scheme: String = {
    request.getScheme
  }

  def host: String = {
    request.getServerName + ":" + request.getServerPort
  }

  def base: String = {
    request.scriptName
  }

  def path: String = {
    base + request.pathInfo
  }

  def url: String = {
    scheme + "://" + host + path
  }
}
