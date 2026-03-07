---
title: "Traefik Dashboard: A Vital Prerequisite for Debugging Pangolin and Middleware Manager - Guides & Tutorials"
source: "https://forum.hhf.technology/t/traefik-dashboard-a-vital-prerequisite-for-debugging-pangolin-and-middleware-manager/2208/12"
author:
  - "[[Mattercoder]]"
published: 2025-05-25
created: 2025-12-29
description: ":globe_with_meridians: Enabling the Traefik Dashboard: A Vital Prerequisite for Debugging Pangolin and Middleware ManagerThe Traefik Dashboard is an essential UI tool for visualizing and debugging your reverse proxy set…"
tags:
  - "clippings"
---
## post by Mattercoder on May 25

1 month later

## post by Daniel on Jun 25

## post by Mattercoder on Jun 25

## post by Daniel on Jun 25

## post by BlackrazorNZ on Jun 28

## post by Mattercoder on Jun 28

## post by BlackrazorNZ on Jun 28

[BlackrazorNZ](https://forum.hhf.technology/u/blackrazornz)

[Jun 28](https://forum.hhf.technology/t/traefik-dashboard-a-vital-prerequisite-for-debugging-pangolin-and-middleware-manager/2208/7?u=ciansedai "Post date")

So I’m an idiot, but i figure i better document my idiocy in case anyone else makes the same mistake.

I have a split DNS setup where traffic to mydomain outside my local network goes via Pangolin, but traffic to \*.mydomain inside my local network goes via NPM and is resolved locally. On my local DNS host(AdGuard Home), pangolin.mydomain is explicitly pointed at my Pangolin VPS.

The issue here was that I hadn’t clicked that I needed to create an explicit DNS record pointing at traefik.mydomain in my local DNS host so that it didn’t get pointed at NPM via the wildcard redirect. So it was trying to resolve an SSL cert for a url that wasn’t even on NPM.

Figured it out when I followed your instructions above on my tablet while sitting at a cafe, and when I got to the Pangolin part I tried the Traefik link one more time just to confirm it was still broken. Worked fine, that’s when I clicked the issue was the split DNS.

With an explicit DNS record in place for traefik.mydomain locally, everything works great. Thanks.

2 months later

## post by OddMagnet on Aug 31

[![](https://forum.hhf.technology/user_avatar/forum.hhf.technology/oddmagnet/96/3241_2.png)](https://forum.hhf.technology/u/oddmagnet)

[OddMagnet](https://forum.hhf.technology/u/oddmagnet)

[Aug 31](https://forum.hhf.technology/t/traefik-dashboard-a-vital-prerequisite-for-debugging-pangolin-and-middleware-manager/2208/8?u=ciansedai "Post date")

THANK YOU!!

I was trying for way too long before I decided to scroll down a bit. I used the guide and initially it was working, which was a lucky coincidence cause I just happened to manually set a DNS server on my laptop to test something.

Later, when I removed the manual DNS server, accessing the VPS’ Traefik Dashboard stopped working, since my Adguard rewrote my query

1 month later

## post by Kerry on Oct 2

[Kerry](https://forum.hhf.technology/u/kerry)

[Oct 2](https://forum.hhf.technology/t/traefik-dashboard-a-vital-prerequisite-for-debugging-pangolin-and-middleware-manager/2208/9?u=ciansedai "Post date")

Very nice I missed the traefik dashboard, works great…thank you.

## post by Kerry on Oct 2

[Kerry](https://forum.hhf.technology/u/kerry)

[Oct 2](https://forum.hhf.technology/t/traefik-dashboard-a-vital-prerequisite-for-debugging-pangolin-and-middleware-manager/2208/10?u=ciansedai "Post date")

Where would you add the label traefik-auth.basicauth.users=admin:$$2y$$05$$8Ue to enable insecure: false

## post by Mattercoder on Oct 3

[Mattercoder](https://forum.hhf.technology/u/mattercoder)

[Oct 3](https://forum.hhf.technology/t/traefik-dashboard-a-vital-prerequisite-for-debugging-pangolin-and-middleware-manager/2208/11?u=ciansedai "Post date")

If you use the middleware manager you can apply the basic auth middleware. Otherwise you will have to look putting the middleware in your traefik’s dynamic\_config.yml

last visit

3 months later

## post by codewhiz 5 days ago

[codewhiz](https://forum.hhf.technology/u/codewhiz)

[5d](https://forum.hhf.technology/t/traefik-dashboard-a-vital-prerequisite-for-debugging-pangolin-and-middleware-manager/2208/12?u=ciansedai "Post date")

Thank you so much [@Mattercoder](https://forum.hhf.technology/u/mattercoder)!

  

### There is 1 new topic remaining, or browse other topics in Guides & Tutorials

[Powered by Discourse](https://discourse.org/powered-by)