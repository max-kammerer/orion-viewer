Local patches applied on top of the third-party checkouts by
`thirdparty_build.gradle` (tasks `patchMupdf` and `patchDjvu`, run after the
matching `download*` task).

Layout mirrors the repository the patch applies to:

    mupdf/*.patch                      -> nativeLibs/mupdf
    mupdf/thirdparty/jbig2dec/*.patch  -> nativeLibs/mupdf/thirdparty/jbig2dec
    djvu/*.patch                       -> nativeLibs/djvu

Patches are applied with `git apply` in name order and skipped if already
applied (`git apply --reverse --check` succeeds). Add a new one as
`NNNN-short-name.patch`.

Upstream submissions with bug reports live in ../../patches.
