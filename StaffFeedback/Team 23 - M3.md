# SwEnt M3 Team Grading Report

This M3 milestone is the culmination of your SwEnt journey, and it gives us the final opportunity to give you, as a team, formal feedback on how you performed in the project. By now, you should be capable of demonstrating a solid command of the Scrum methodology and collaborative teamwork, and be able to deliver a high-quality application that is ready for real users.
This feedback report is meant to complement the informal, ungraded feedback that you received from your coaches during the weekly meetings, over email, on Discord, etc.

You can find the evaluation criteria in the [M3 Deliverables](https://github.com/swent-epfl/public/blob/main/project/M3.md) document.
As mentioned before, the standards for M2 were elevated relative to M1, and this progression continued into M3: we now hold you to the highest professional standard in SwEnt.

---

## Blue Belt

You qualified for a Blue Belt 🥋🔵 and got a final team grade of 5.4/6 for M3. Excellent work! You're demonstrating advanced skills.

We looked at several aspects, grouped into seven categories for the team grade. Here is the breakdown of the points you earned:

| Metric                                   | **Points Earned**                | **Weight** | **Feedback**                               |
|------------------------------------------|----------------------------------|------------|--------------------------------------------|
| **Completeness**                         | 5.2 out of 6 | 20%        | [See Details](#completeness)               |
| **Functionality**                        | 5.1 out of 6 | 20%       | [See Details](#functionality)              |
| **User Experience**                      | 4.9 out of 6 | 5%       | [See Details](#user-experience)            |
| **Design and Maintainability**           | 5.25 out of 6 | 20% | [See Details](#design-and-maintainability) |
| **QA (Testing)**                         | 5.96 out of 6           | 20%        | [See Details](#qa-testing)                 |
| **Documentation**                        | 5.55 out of 6 | 10%       | [See Details](#documentation)              |
| **Autonomy**                             | 6 out of 6     | 5%         | [See Details](#autonomy)                   |
| **Final Team Grade**                     | **5.4 out of 6** |            |                                            |

In addition to the feedback you received from the coaches during the Sprints, you will find more detailed comments below.

---

## Completeness

We first evaluated the depth and complexity of the main epics in your app, along with their contribution to the app, the tangible value they provide to the user, and their alignment with the app’s goals.

We evaluated the extent to which your app meets the course requirements articulated at the start of the semester, and whether they are implemented effectively, they integrate seamlessly, and are indeed essential to the app.

We also looked at the robustness and completeness of the different features you implemented: are all the features finished and polished, are they secure and bug-free, and are they thoughtfully designed.

- The EPICs you wanted to add to your app were implemented as your user stories suggested. Content wise, you could have decided to augment some of them with more sub-features as the number of actions on the app is a bit limited.

- The offline mode is supported, but there is a small banner that is spammed on the screen every 5 seconds. Most of the feature are fully locked, it could have been good to for example being able to add a plant while being offline, and then having it published to your account when connections resumes. Also, putting back on the wifi was not detected by your app, we had to fully close it and open it again (not good for minor connection interruptions).

- The features that were implemented were working as expected in most of the cases, but for example, editing a plant that was recognized by your AI is impossible (description etc.), which is a bit limiting if the AI is constantly getting it wrong. (Not necessarily the case, but might be). 


For this part, you received 5.2/6.

---

## Functionality

In this context, we assessed your app's ability to handle unexpected inputs provided by clueless or malicious users (including spamming buttons, entering wrong inputs, stopping a process mid-way, etc.); we wanted to see that your app handles all edge cases gracefully, has comprehensive error handling, and includes robust mechanisms for maintaining stability under stress.

We then evaluated the performance and reliability of the final product, i.e., the APK: we wanted to see that your APK is stable and the UI responds quickly and has seamless navigation.

Next we looked into your implementation of user authentication and multi-user support: does the app correctly manage users, can users personalize their accounts, does the app support session persistence, are multi-user interactions well supported, can accounts be used on another device, and is account information preserved when switching devices.

- The logout button of your App makes it crash.

- When prompted for camera permission, if the user responds "only once", the prompt is never shown again and you have to go through your settings to set it as "always on" (the status in the setting is "ask everytime" so it should technically work)

- If you delete a plant, it disappears from your garden, but not from the feed. We can still click on it and it gives a "ghost" screen.

- Some of the features are a bit simple and could have been expanded with more complex behaviour. (the friends search bar could retrieve the list of friends automatically without the need to click on the search button for example)

For this part, you received 5.1/6.

---

## User Experience

For this part, we wanted to see how intuitive and user-friendly the app is for real users. Beyond having good usability, did you pay attention to streamlining the interactions, is it easy to figure out, can new users start making good use of the app quickly, are the interaction flows well thought out and refined.

- An onboarding feature to briefly explain how your app is used could have been good.

- You could have added more visual feedback on the current state of the user's interactions. For example, if the permission is not given for the camera, you could add a small text explaining why it is needed instead of the two buttons that you have. The same goes for the search friend screen, there could be a small text describing the current state of the search.

- Concerning the UI structure of your app, it is a bit different to what we are used to see in standard apps and this makes some actions not entirely natural. (add friend button in the feed could be in the screen dedicated to friends for example)



For this part, you received 4.9/6.

---

## Design and Maintainability

We evaluated whether your code is of high quality and employs best practices.
We expect the codebase to be polished, well documented, follow consistent conventions, be modular, and allow for easy modifications.
You should be able to employ advanced techniques by now, such as asynchronous functions (flows, coroutines), good resource management, and automated dependency injection (e.g., with Hilt).

We also assessed your overall app architecture and design, looking in particular at aspects surrounding robustness and scalability.
We looked at both the codebase and the documentation of the app (Wiki and architecture diagram), we expect your design to demonstrate thoughtful consideration for performance, maintainability, and future growth.

- The MainActivity file contains too many functions, the MyGardenApp could have been put into another file for better readability.

- Some parts your architecture are missing the ViewModel layer (e.g. sign-out logic, offline logic, ...)

For this part, you received 5.25/6.

---

## QA (Testing)

The first aspect we looked at here was your test suite, in terms of both quality and the final line coverage.
We expect the testing to be rigorous and to cover all components and edge cases, and they should validate every significant user journey.
Your end-to-end tests should be detailed and include error-handling scenarios.
The tests should be well-documented and easy to maintain.
Finally, your test suite should demonstrate advanced techniques, mock data for performance testing, and automated regression tests.

- The project makes excellent use of "Fakes" (e.g., FakePlantsRepository, FakeProfileRepository) to isolate components and ensure fast, deterministic tests without relying on a live network. Good job!

For this part, you received 5.96/6.

---

## Documentation

We looked at your README and GitHub Wiki to evaluate the quality and completeness of your app’s documentation. We expect the README and Wiki to be thorough and achieve professional-level clarity and completeness.
They should provide detailed descriptions of the app's architecture, implementation of the features, the development setup and guidelines for contributing.

We also assessed your use of Figma and the architecture diagram for effective UI design, organization, and app structure planning.
By this stage, we expect your Figma and Architecture diagram to be complete and up-to-date. The Figma should be consistent with the UI and the Architecture diagram should be comprehensive.

- The Figma designs and the final app differ, leading to UI inconsistencies. Figma should ideally be production-ready and used as the single source of truth, making implementation easier and ensuring consistent UI across the app.

- Some tests do not have enough documentation.

For this part, you received 5.55/6.

---

## Autonomy

A primary goal of SwEnt is to teach you how to function autonomously as a team.
For this part of the evaluation, we assessed you team’s independence, spanning Sprint 6 to Sprint 10, based on the meetings with coaches, Sprint planning, and how you managed risk.
By this stage, coaches should no longer be necessary for the team to operate, i.e., you can organize yourselves, you don't need to be reminded about tasks, and you can conduct the Scrum ceremonies on your own.



For this part, you received 6/6.

---

## Summary

Based on the above points, your grade for this M3 milestone is 5.4/6. If you are interested in how this fits into the bigger grading scheme, please see [project README](https://github.com/swent-epfl/public/blob/main/project/README.md) and the [course README](https://github.com/swent-epfl/public/blob/main/README.md).

The entire SwEnt staff wishes you the very best in your career, and we look forward to seeing you do great things with what you learned this semester.



