# lein-subscribable-urls

A Leiningen plugin that lists all the `:url`s contained in your dependency graph that you can subscribe to via RSS (namely: GitHub ones), emitting Slack-friendly subscription code.

> Slack features an [official RSS client](https://slack.com/apps/A0F81R7U7-rss).

## Installation

Add

```clojure
[lein-subscribable-urls "unreleased"]
```

to the `:plugins` vector of your `:user` profile in `~/.lein/profiles.clj`.

> Not compatible with Leiningen 1.x.

## Usage

Run `lein subscribable-urls :format {feed,curl} :recursive {true,false}` in your project directory:

```bash
$ lein subscribable-urls :format feed :recursive true
/feed https://github.com/AvisoNovate/pretty/releases.atom
/feed https://github.com/FasterXML/jackson-core/releases.atom
/feed https://github.com/bhb/expound/releases.atom
# (more...)

$ lein subscribable-urls :format curl :recursive true
curl -X POST "https://slack.com/api/chat.command?token=$TOKEN&channel=$CHANNEL_ID&command=/feed&text=https://github.com/AvisoNovate/pretty/releases.atom&pretty=1"
curl -X POST "https://slack.com/api/chat.command?token=$TOKEN&channel=$CHANNEL_ID&command=/feed&text=https://github.com/FasterXML/jackson-core/releases.atom&pretty=1"
curl -X POST "https://slack.com/api/chat.command?token=$TOKEN&channel=$CHANNEL_ID&command=/feed&text=https://github.com/bhb/expound/releases.atom&pretty=1"
# (more ...)
```

The `:recursive` option controls whether transitive dependencies will be printed.

* You can use the `/feed` output directly, copying into a Slack channel, line by line.
* You can use the `curl` output to build a simple Bbash script that executes those `curl` invocations sequentially. You are responsible for setting `TOKEN` and `CHANNEL` beforehand.  

## Analyzing all your projects

cd into your "code" dir (which can also be `~`), fire up `irb` (the Ruby console), and run:

```ruby
Dir['*'].select{|f| File.directory?(f) && File.exist?(f + "/project.clj")}.map{|f| `cd #{f}; lein do deps, subscribable-urls :format feed :recursive false`.split("\n") }.flatten(1).uniq.sort.each{|a| puts a}; nil
```

## Credit

This project takes [lein-licenses](https://github.com/technomancy/lein-licenses) as a baseline. It's not a fork since its git history barely helps understanding this project's different purpose.

## License

Copyright © 2012-2017 Phil Hagelberg and contributors
Copyright © 2020- Nedap

Distributed under the Eclipse Public License, the same as Clojure.
