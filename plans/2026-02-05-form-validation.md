# Form Validation Support for uni-dom

## Overview

Add composable, reactive form validation that integrates naturally with the existing `value.bind()` / `checked.bind()` two-way binding system. Validation is purely Rx-based - no new DomNode types or DomRenderer changes needed.

## API Design

### Core Types

- `ValidationState` - enum with `Valid` and `Invalid(messages)`, with `isValid` and `errors` accessors
- `ValidationRule[A]` - trait with `validate(A): ValidationState`
- `FieldValidation[A]` - reactive validation for a single field, derives `state`/`isValid`/`errors` via `source.map()`
- `FormValidation` - aggregates multiple field validations using chained `Rx.join`
- `Validate` object - factory methods + built-in validators

### Built-in Validators

- `required(message)` - non-empty string
- `minLength(n, message)` - minimum character count
- `maxLength(n, message)` - maximum character count
- `pattern(regex, message)` - regex matching
- `email(message)` - email format (simple practical regex)
- `rule[A](predicate, message)` - custom predicate
- `ruleWith[A](f)` - custom function returning ValidationState

## Files

| File | Description |
|------|-------------|
| `uni/.js/src/main/scala/wvlet/uni/dom/Validate.scala` | All validation types and logic |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/ValidateTest.scala` | 21 tests |
| `uni/.js/src/main/scala/wvlet/uni/dom/all.scala` | Added exports |

## Key Design Decisions

- Purely Rx-based: validation state is plain `Rx` values consumed by existing reactive DOM binding
- Validation runs on every change by default (reacts to all updates from `value.bind()`)
- FormValidation uses chained pairwise `Rx.join` via `foldLeft` to support any number of fields
- Tracks last value internally for imperative `validateNow()`/`validateAll()`; falls back to reading `RxVar.get` directly when the reactive chain has not been subscribed to
- Pattern follows Storage.scala: self-contained module, no DomNode extension needed
