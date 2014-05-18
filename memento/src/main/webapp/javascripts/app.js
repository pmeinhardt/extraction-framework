(function() {
    'use strict';

    var LinkFormat = function() {};

    LinkFormat.prototype.parse = function(data) {
        var links = data.match(/<([^>]*)>(\s*;\s*([\S]+)="([^"]*)")*,?/g).map(function(entry) {
            var href = entry.match(/<([^>]*)>/)[1];
            var link = { href: href };

            var attrs = entry.split(/;\s*/g);
            attrs.shift(); // remove href

            attrs = attrs.map(function(section) {
                var parts = section.match(/([\S]+)="([^"]+)"/);
                return { key: parts[1], value: parts[2] };
            });

            attrs.forEach(function(attr) {
                link[attr.key] = attr.value;
            });

            return link;
        });

        return links;
    };

    var MementoClient = function() {};

    MementoClient.prototype.getTimeMap = function(url, cb) {
        var req = $.ajax({ url: url });

        req.done(function(data) {
            var format = new LinkFormat();
            cb(format.parse(data));
        });
    };

    var Revision = React.createClass({
        render: function() {
            var children = [];

            var label = moment(this.props.timestamp).fromNow();

            children.push(React.DOM.div({
                className: 'relative',
                children: [React.DOM.p({
                    className: 'revision-label'
                }, '#' + this.props.index), React.DOM.span({
                    className: 'date'
                }, label), React.DOM.span({
                    className: 'circle'
                })]
            }));

            children.push(React.DOM.div({
                className: 'content',
                children: [React.DOM.a({
                    href: this.props.href,
                    target: '_blank'
                }, 'view'),
                ' or ',
                React.DOM.a({
                    href: this.props.href,
                    download: 'triples.nt'
                }, 'download')]
            }));

            return React.DOM.li({
                className: 'revision'
            }, children);
        }
    });

    var Timeline = React.createClass({
        render: function() {
            var revisions = this.props.revisions;
            var count = revisions.length;

            return React.DOM.ul({
                className: 'timeline',
                children: revisions.map(function(rev, i) {
                    return Revision(rev);
                })
            });
        }
    });

    $('#search-form').on('submit', function(e) {
        e.preventDefault();

        var id = parseInt($(this).find('[name="resource-id"]').val(), 10);
        var client = new MementoClient();

        client.getTimeMap('/pages/' + id + '/history', function(links) {
            var revisions;

            links = links.filter(function(l) { return (l.rel == 'memento'); });

            revisions = links.map(function(link, i, links) {
                var date = new Date(link.datetime);
                return {
                    timestamp: date.getTime(),
                    index: links.length - i,
                    href: link.href
                };
            });

            React.renderComponent(Timeline({revisions: revisions}), document.getElementById('timeline'));
        });

        return false;
    });
})();
