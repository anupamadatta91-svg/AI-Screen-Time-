# Project Description

## Title

**AI-Based Mobile Screen Time Monitoring and Digital Wellbeing Assistant**

## Problem

Smartphone users may spend excessive time on social media, entertainment, games, or other applications without realizing how much time is being consumed. Android provides application usage statistics, but users need a simple system that converts those statistics into understandable insights and alerts.

## Objective

The objective is to develop an Android application that:

- measures daily mobile application usage;
- identifies the applications with the highest usage;
- compares usage with a user-defined limit;
- detects potentially excessive usage patterns;
- generates explainable AI-based recommendations;
- provides an alert mechanism when the configured limit is exceeded.

## Proposed architecture

User
→ Android application
→ UsageStatsManager
→ Usage data processor
→ Feature extraction
→ AI/risk scoring engine
→ Dashboard + recommendation
→ Notification alert

## Input features

Example features include:

- total daily app usage;
- most-used app duration;
- percentage of total time spent in the most-used app;
- number of apps used for more than one hour;
- configured daily limit.

## Output

- total usage;
- app-by-app usage;
- usage percentage;
- risk score;
- risk category;
- personalized recommendations;
- limit-crossed alert.

## AI component

The prototype implements an explainable scoring model rather than a black-box model. This is appropriate for a first academic prototype because every prediction can be explained to the lecturer.

A later version can train a supervised model using anonymized daily usage records. Possible labels are `normal`, `moderate`, and `high`. Candidate models include logistic regression, decision tree, random forest, or a small neural network.

## Ethics and privacy

The application should process usage information locally where possible. It should not read private messages, passwords, keyboard content, or screen images. The user should explicitly enable Usage Access and understand what data the application reads.
