test_name 'C3491 - checkout as a group (https protocol)'

# Globals
repo_name = 'testrepo_group_checkout'
group = 'mygroup'

hosts.each do |host|
  ruby = (host.is_pe? && '/opt/puppet/bin/ruby') || 'ruby'
  tmpdir = host.tmpdir('vcsrepo')
  step 'setup - create repo' do
    install_package(host, 'git')
    my_root = File.expand_path(File.join(File.dirname(__FILE__), '../../../..'))
    scp_to(host, "#{my_root}/acceptance/files/create_git_repo.sh", tmpdir)
    on(host, "cd #{tmpdir} && ./create_git_repo.sh")
  end
  step 'setup - start https server' do
    https_daemon =<<-EOF
    require 'webrick'
    require 'webrick/https'
    server = WEBrick::HTTPServer.new(
    :Port               => 8443,
    :DocumentRoot       => "#{tmpdir}",
    :SSLEnable          => true,
    :SSLVerifyClient    => OpenSSL::SSL::VERIFY_NONE,
    :SSLCertificate     => OpenSSL::X509::Certificate.new(  File.open("#{tmpdir}/server.crt").read),
    :SSLPrivateKey      => OpenSSL::PKey::RSA.new(          File.open("#{tmpdir}/server.key").read),
    :SSLCertName        => [ [ "CN",WEBrick::Utils::getservername ] ])
    WEBrick::Daemon.start
    server.start
    EOF
    create_remote_file(host, '/tmp/https_daemon.rb', https_daemon)
    #on(host, "#{ruby} /tmp/https_daemon.rb")
  end

  step 'setup - create group' do
    apply_manifest_on(host, "group { '#{group}': ensure => present, }", :catch_failures => true)
  end

  teardown do
    on(host, "rm -fr #{tmpdir}")
    on(host, "ps ax | grep '#{ruby} /tmp/https_daemon.rb' | grep -v grep | awk '{print \"kill -9 \" $1}' | sh ; sleep 1")
    apply_manifest_on(host, "group { '#{group}': ensure => absent, }", :catch_failures => true)
  end

  step 'checkout as a group with puppet' do
    pp = <<-EOS
    vcsrepo { "#{tmpdir}/#{repo_name}":
      ensure => present,
      source => "https://github.com/johnduarte/testrepo.git",
      provider => git,
      group => '#{group}',
    }
    EOS

    apply_manifest_on(host, pp, :catch_failures => true)
    apply_manifest_on(host, pp, :catch_changes  => true)
  end

  step "verify git checkout is own by group #{group}" do
    on(host, "ls #{tmpdir}/#{repo_name}/.git/") do |res|
      fail_test('checkout not found') unless res.stdout.include? "HEAD"
    end

    on(host, "stat --format '%U:%G' #{tmpdir}/#{repo_name}/.git/HEAD") do |res|
      fail_test('checkout not owned by group') unless res.stdout.include? ":#{group}"
    end
  end

end
