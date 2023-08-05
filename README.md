# Tasks (API Backend)

A Kotlin-based backend for my functional and colorful tasks app.

_Looking for the [mobile](https://github.com/19lmyers/tasks-kmm) or [web](https://github.com/19lmyers/tasks.chara.dev)
apps?_

## Objectives

From the beginning Tasks (KMM) was conceived of as a cloud-based task list app.
This required implementing my own backend to store user data.

I decided early on to roll my own account system (as opposed to implementing social login) as I wanted to practice
safe user secret handling.

## Libraries

I decided to build the backend with Ktor, as it seemed like a really great tool for server side development, and it's
been quite pleasant to use thus far.

Over time, I also incorporated Firebase Cloud Messaging (FCM) and Quartz Scheduler to power reminders and token expiry.

## Architecture

The backend has changed a lot since its initial version. The biggest change by far has been incorporating
Railway-Oriented Programming (ROP) to make critical code paths easier to follow.

### Authentication

The authentication system uses a JWT-based token to authenticate users, alongside a second refresh token that's longer
lived.
This design requires client applications to manually persist and refresh tokens, but is altogether easy to implement.

# Developer note

For code linting before each commit, change the project's git hooks directory with this command:

```shell
git config core.hooksPath hooks/
```

You can also copy the contents of /hooks to your project's git hooks folder.