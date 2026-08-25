# H2pro

نظام محاسبي عربي يعمل على Android.

## البناء

يتم بناء نسخة Debug تلقائياً عبر GitHub Actions عند كل Push إلى `main`، ويمكن تشغيل Workflow يدوياً من تبويب Actions.

## APK

بعد نجاح البناء ستجد `H2pro-debug-apk` ضمن Artifacts في تشغيل Workflow.

## الحالة الحالية

تم إصلاح هيكل مشروع Android، إعداد Gradle، نقطة تشغيل التطبيق، وWorkflow الخاص ببناء APK.
