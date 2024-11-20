1. [ ] Check for `TODO` and `STOPSHIP`
2. [ ] Commit and push
3. [ ] Bump the version in `app/version.properties`
4. [ ] Write a changelog on poeditor and tag it with `ignore-string-android` and `fastlane-android`
5. [ ] Audit translations
6. [ ] Run `ulimit -n 2048 && bundle exec fastlane pre_release`
7. [ ] Verify the screenshots
8. [ ] Commit and tag
9. [ ] Push and push tags
10. [ ] Run `SUPPLY_JSON_KEY=<path> bundle exec fastlane production:<bool> release`

See `fastlane/Fastfile` comments for debugging information on Fastlane crashes.
