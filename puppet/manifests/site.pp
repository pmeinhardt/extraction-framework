node default {
  include stdlib
  include apt
  include java7

  Exec {
    path => "/usr/bin:/bin:/usr/sbin:/sbin"
  }

  # exec { "apt-update":
  #   command => "/usr/bin/apt-get -y update"
  # }

  package { "git-core":
    ensure => installed
  }

  package { "maven":
    ensure => installed
  }

  # Create the required local databases

  class { "::mysql::server":
    databases => {
      "dbpedia_live" => {
        ensure  => present,
        charset => "utf8"
      },
      "dbpedia_live_cache" => {
        ensure  => present,
        charset => "utf8"
      }
    },
    users => {
      "mediawiki@localhost" => {
        ensure        => present,
        password_hash => mysql_password("mediawiki")
      }
    },
    grants => {
      "mediawiki@localhost/dbpedia_live.*" => {
        ensure     => present,
        options    => ["GRANT"],
        privileges => ["CREATE", "INDEX", "SELECT", "INSERT", "UPDATE", "DELETE"],
        table      => "dbpedia_live.*",
        user       => "mediawiki@localhost"
      }
    },
    restart => true
  }

  # Download and install MediaWiki

  archive { "mediawiki-1.23.0":
    ensure        => present,
    url           => "http://releases.wikimedia.org/mediawiki/1.23/mediawiki-1.23.0.tar.gz",
    digest_string => "a0d979742b5fc83aef828d8e96bba266956b5baf",
    digest_type   => "sha1",
    checksum      => true,
    extension     => "tar.gz",
    target        => "/var/www", # => "/var/www/mediawiki-1.23.0"
    src_target    => "/tmp"
  }

  exec { "mysql dbpedia_live < /var/www/mediawiki-1.23.0/maintenance/tables.sql":
    subscribe   => Archive["mediawiki-1.23.0"],
    refreshonly => true
  }

  # TODO: Download and import a Wikipedia dump for your local Wikiedia mirror

  # TODO: Setup OAI (http://git.io/QvJM6Q)

  vcsrepo { "/var/www/mediawiki-1.23.0/extensions/OAI":
    ensure   => present,
    provider => git,
    source   => "http://git.wikimedia.org/git/mediawiki/extensions/OAI.git",
    revision => "master"
  }

  # exec { "mysql dbpedia_live < /var/www/mediawiki-1.23.0/extensions/OAI/update_table.sql …":
  #   subscribe   => Vcsrepo["/var/www/mediawiki-1.23.0/extensions/OAI"],
  #   refreshonly => true
  # }

  # TODO: Setup live cache (http://git.io/bStaLg)

  # exec { "mysql dbpedia_live_cache < /home/vagrant/extraction-framework/live/src/main/SQL/dbstructure.sql":
  #   subscribe   => ???,
  #   refreshonly => true
  # }

  # Configure Apache (http://git.io/_k2wZQ)

  package { "php5":
    ensure => installed
  }

  package { "php5-mysql":
    ensure => installed
  }

  package { "libapache2-mod-php5":
    ensure => installed
  }

  class { "apache":
    mpm_module   => "prefork" # required by apache::mod::php
  }

  include apache::mod::php

  apache::vhost { "dbpedia-live":
    docroot => "/var/www/mediawiki-1.23.0",
    port    => "8080"
  }

  # Optional, development packages

  # package { "vim":
  #   ensure => installed
  # }
}
