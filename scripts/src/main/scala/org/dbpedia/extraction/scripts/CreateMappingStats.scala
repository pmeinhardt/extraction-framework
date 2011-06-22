package org.dbpedia.extraction.scripts

import io.Source
import java.lang.IllegalArgumentException
import java.io._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.wikiparser.WikiTitle

/**
 * Script to gather statistics about mappings: how often they are used, which properties are used and for what mappings exist.
 */
object CreateMappingStats
{
    val lang = "en"
    private val language = Language.fromWikiCode(lang).get

    private val templateNamespacePrefix = Namespaces.getNameForNamespace(language, WikiTitle.Namespace.Template) + ":"
    private val resourceNamespacePrefix = if (lang == "en") "http://dbpedia.org/resource/" else "http://" + lang + "dbpedia.org/resource/"

    private val ObjectPropertyTripleRegex = """<([^>]+)> <([^>]+)> <([^>]+)> .""".r
    private val DatatypePropertyTripleRegex = """<([^>]+)> <([^>]+)> "(.+?)"@?\w?\w? .""".r

    // Hold template redirects and template statistics
    @serializable
    private class WikipediaStats(var redirects : Map[String,String] = Map(), var templates : Map[String,TemplateStats] = Map())

    // Hold template statistics
    @serializable
    private class TemplateStats(var templateCount : Int = 0, var properties : Map[String,Int] = Map())
    {
        override def toString = "TemplateStats[count:"+templateCount+",properties:"+properties.mkString(",")+"]"
    }

    def main(args : Array[String])
    {
        val serializeFileName = args(0)
        val redirectsDatasetFileName = args(1)
        val infoboxPropertiesDatasetFileName = args(2)
        val templateParametersDatasetFileName = args(3)
        val infoboxTestDatasetFileName = args(4)

        var wikiStats : WikipediaStats = null

        val startTime = System.currentTimeMillis()
        if(new File(serializeFileName).isFile)
        {
            println("Loading serialized object from " + serializeFileName)
            wikiStats = deserialize(serializeFileName)
        }
        else
        {
            wikiStats = getWikipediaStats(redirectsDatasetFileName, infoboxPropertiesDatasetFileName, templateParametersDatasetFileName, infoboxTestDatasetFileName)
            println("Serializing to " + serializeFileName)
            serialize(serializeFileName, wikiStats)
        }

        //TODO everything below

        // load data from mappings wiki


        // compare the result map to the output from the mappings wiki

        // - mapped infobox name is not found in Wikipedia stats (name has changed)
        //     if is redirect now -> schedule for infobox mapping renaming
        //     else -> schedule for investigation

        // - template is not mapped on the wiki
        //     schedule for infobox mapping (sorted by templateCount)

        // - missing property mappings
        //     schedule for property mapping (sorted by propertyCount)

        // - property is mapped that is not found with a template in Wikipedia stats !TemplateStats.properties.contains(mappedProperty)
        //     schedule for property deletion


        // produce and write out a table


        println((System.currentTimeMillis()-startTime)/1000 + " s")
    }


    private def getWikipediaStats(redirectsFile : String, infoboxPropsFile : String, templParsFile : String, parsUsageFile : String) : WikipediaStats =
    {
        var templatesMap : Map[String,TemplateStats] = Map()  // "templateName" -> TemplateStats

        println("Reading redirects from " + redirectsFile)
        val redirects : Map[String,String] = loadTemplateRedirects(redirectsFile)
        println("  " + redirects.size + " redirects")

        println("Using Template namespace prefix " + templateNamespacePrefix + " for language " + lang)
        println("Counting templates in " + infoboxPropsFile)
        templatesMap = countTemplates(infoboxPropsFile, templatesMap, redirects)
        println("  " + templatesMap.size + " different templates")

        println("Loading property definitions from " + templParsFile)
        templatesMap = propertyDefinitions(templParsFile, templatesMap, redirects)

        println("Counting properties in " + parsUsageFile)
        templatesMap = countProperties(parsUsageFile,templatesMap, redirects)

        new WikipediaStats(redirects, templatesMap)
    }


    private def stripUri(fullUri : String) : String =
    {
        fullUri.replace(resourceNamespacePrefix, "")
    }

    private def loadTemplateRedirects(fileName : String) : Map[String,String] =
    {
        var redirects : Map[String,String] = Map()
        for(line <- Source.fromFile(fileName, "UTF-8").getLines())
        {
            line match
            {
                case ObjectPropertyTripleRegex(subj, pred, obj) =>
                {
                    val templateName = stripUri(subj)
                    if(templateName startsWith templateNamespacePrefix)
                    {
                        redirects = redirects.updated(templateName, stripUri(obj))
                    }
                }
                case _ if line.nonEmpty => throw new IllegalArgumentException("line did not match redirects syntax: "+line)
                case _ =>
            }
        }

        // resolve transitive closure
        for((source,target) <- redirects)
        {
            var cyclePrevention : Set[String] = Set()
            var closure = target
            while( redirects.contains(closure) && !cyclePrevention.contains(closure) )
            {
                closure = redirects.get(closure).get
                cyclePrevention += closure
            }
            redirects = redirects.updated(source, closure)
        }

        redirects
    }

    private def countTemplates(fileName : String, resultMap : Map[String,TemplateStats], redirects : Map[String,String]) : Map[String,TemplateStats] =
    {
        var newResultMap = resultMap
        // iterate through infobox properties
        for(line <- Source.fromFile(fileName, "UTF-8").getLines())
        {
            line match
            {
                // if there is a wikiPageUsesTemplate relation
                case ObjectPropertyTripleRegex(subj, pred, obj) if pred contains "wikiPageUsesTemplate" =>
                {
                    // resolve redirect for *object*
                    val templateName = redirects.get(stripUri(obj)).getOrElse(stripUri(obj))

                    // lookup the *object* in the resultMap;   create a new TemplateStats object if not found
                    val stats = newResultMap.get(templateName).getOrElse(new TemplateStats)

                    // increment templateCount
                    stats.templateCount += 1

                    newResultMap = newResultMap.updated(templateName, stats)
                }
                case _ =>
            }
        }
        newResultMap
    }

    private def propertyDefinitions(fileName : String, resultMap : Map[String,TemplateStats], redirects : Map[String,String]) : Map[String,TemplateStats] =
    {
        var newResultMap = resultMap
        // iterate through template parameters
        for(line <- Source.fromFile(fileName, "UTF-8").getLines())
        {
            line match
            {
                case DatatypePropertyTripleRegex(subj, pred, obj) =>
                {
                    // resolve redirect for *subject*
                    val templateName = redirects.get(stripUri(subj)).getOrElse(stripUri(subj))

                    // lookup the *subject* in the resultMap
                    newResultMap.get(templateName) match
                    {
                        case Some(stats : TemplateStats) =>
                        {
                            // add object to properties map with count 0
                            stats.properties = stats.properties.updated(stripUri(obj), 0)
                            newResultMap = newResultMap.updated(templateName, stats)
                        }
                        case None => //skip the templates that are not found (they don't occurr in Wikipedia)
                    }
                }
                case _ if line.nonEmpty => throw new IllegalArgumentException("line did not match property syntax: "+line)
                case _ =>
            }
        }
        newResultMap
    }

    private def countProperties(fileName : String, resultMap : Map[String,TemplateStats], redirects : Map[String,String]) : Map[String,TemplateStats] =
    {
        var newResultMap = resultMap
        // iterate through infobox test
        for(line <- Source.fromFile(fileName, "UTF-8").getLines())
        {
            line match
            {
                case DatatypePropertyTripleRegex(subj, pred, obj) =>
                {
                    // resolve redirect for *predicate*
                    val templateName = redirects.get(stripUri(pred)).getOrElse(stripUri(pred))

                    // lookup the *predicate* in the resultMap
                    newResultMap.get(templateName) match
                    {
                        case Some(stats : TemplateStats) =>
                        {
                            // lookup *object* in the properties map
                            stats.properties.get(stripUri(obj)) match
                            {
                                case Some(oldCount : Int) =>
                                {
                                    // increment count in properties map
                                    stats.properties = stats.properties.updated(stripUri(obj), oldCount + 1)
                                    newResultMap = newResultMap.updated(templateName, stats)
                                }
                                case None => //skip the properties that are not found with any count (they don't occurr in the template definition)
                            }
                        }
                        case None => //skip the templates that are not found (they don't occurr in Wikipedia)
                    }
                }
                case _ if line.nonEmpty => throw new IllegalArgumentException("line did not match countProperties syntax: "+line)
                case _ =>
            }
        }
        newResultMap
    }

    private def serialize(fileName : String, wikiStats : WikipediaStats)
    {
        val output = new ObjectOutputStream(new FileOutputStream(fileName))
        output.writeObject(wikiStats)
        output.close()
    }

    private def deserialize(fileName : String) : WikipediaStats =
    {
        val input = new ObjectInputStream(new FileInputStream(fileName))
        val m = input.readObject()
        input.close()
        m.asInstanceOf[WikipediaStats]
    }

}