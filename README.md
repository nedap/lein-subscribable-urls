# lein-subscribable-urls

A Leiningen plugin that lists all the `:url`s contained in your dependency graph that you can subscribe to via RSS (namely: GitHub ones), emitting Slack-friendly subscription code.

> Slack features an [official RSS client](https://slack.com/apps/A0F81R7U7-rss).

## Rationale

It's great to keep up with projects, so one can decide to upgrade. Or at least one can be aware of new developments, even if not wanting to immediately upgrade.

I also wanted to show how the current state of things in [Clojurians Slack](http://clojurians.net/) `#announcements` isn't exactly ideal:
things could be a bit more automated, and customized (individuals/teams decide which news to receive).

Similarly it's worth pointing out that the Github Releases page can serve as a great medium for changelogs.

## Alternatives

* An alternative is integrating [lein-ancient](https://github.com/xsc/lein-ancient) into your workflow.
However upgrading blindly it's not a particularly informed approach, and may lead to security risks ([e.g.](https://blog.npmjs.org/post/180565383195/details-about-the-event-stream-incident)).
* [lein-nvd](https://github.com/rm-hull/lein-nvd) has a slight overlap with this library, but both plugins are certainly not exclusive.

## Installation

Add:

```clojure
[lein-subscribable-urls "0.1.0-alpha1"]
```

to the `:plugins` vector of your `:user` profile in `~/.lein/profiles.clj`.

> Not compatible with Leiningen 1.x.

## Usage

Run `lein subscribable-urls :format {feed,curl,plain} :recursive {true,false}` in your project directory:

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

> In case you make heavy use of Lein profiles, make sure to prefix the task invocation with your targeted profiles - e.g. `lein with-profile +test subscribable-urls ...`.
> That way, the dependencies specific to those profiles will also show up.

The `:recursive` option controls whether transitive dependencies will be printed.

* You can use the `/feed` output directly, copying it into a Slack channel, line by line.
* You can use the `curl` output to build a simple Bash script that executes those `curl` invocations sequentially. You are responsible for setting `TOKEN` and `CHANNEL` beforehand.  
* `:format plain` prints plain URLs, without any Slack-related wrapping whatsoever.

## Slack integration in practice

A workflow I would recommend is creating a `#clojure-libs-rss` channel in your Slack at work. Maybe it can be muted. That way one can keep up with news e.g. once a week.

## Caveats

* Not every GitHub repo uses its `releases` functionality
* This plugin only covers GitHub-backed projects: GitLab is missing (it doesn't serve RSS) as are projects setting any other `:url` (including `nil`)
* Private repos (even if yours) will be unreachable

## Analyzing all your projects

cd into your "code" dir (which can also be `~`), fire up `irb` (the Ruby console), and run:

```ruby
Dir['*'].select{|f| File.directory?(f) && File.exist?(f + "/project.clj")}.map{|f| `cd #{f}; lein with-profile +test do deps, subscribable-urls :format feed :recursive false`.split("\n") }.flatten(1).uniq.sort.each{|a| puts a}; nil
```

...make sure to tweak the inner `lein` per your liking.

## Credit

This project takes [lein-licenses](https://github.com/technomancy/lein-licenses) as a baseline. It's not a fork since its git history barely helps understanding this project's different purpose.

## License

Copyright © 2012-2017 Phil Hagelberg and contributors

Copyright © 2020- Nedap

This program and the accompanying materials are made available under the terms of the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0)
