# Moar CLI

Module Mangement *and moar!*  

## Overview

The **Moar CLI** tool manages multi-module builds without using traditional [Git Submodules](https://git-scm.com/book/en/v2/Git-Tools-Submodules).

With traditional [Git Submodules](https://git-scm.com/book/en/v2/Git-Tools-Submodules) every application that uses a module has it's own seperate copy.  

With the **Moar CLI** multi-module builds are managed using modules that exist at the top level and are linked to consumers via [symbolic links](https://en.wikipedia.org/wiki/Ln_(Unix)).  This provides a benefit when mulitple modules use the same source module.  The typical use case is when several micro services use various library modules.  With the symbolic link approach developers only need to manage one copy of each library module regardless of how many applications are using the module.   

A developer with two libraries used by six applications the [Git Submodules](https://git-scm.com/book/en/v2/Git-Tools-Submodules) must manage eighteen repositories on their workstations.  Keepoing all the modules in sync with pulls and pushes can be quite a chore.  Using **Moar CLI** the environment is managed with only the eight top level modules.

## Usage

All modules are managed in `~/moar-workspace`.  

The workflow for **Moar CLI** supports developers who use traditional paterns for Git usage.  In particular the tool assumes that development work performed locally is pushed to a users fork and eventually the origin develop branch.

## Install

```bash
npm install -g moar-cli
``` 

## Tutorial

To get started, fork the `moar-sugar-example`.

This tutorial works on **macOS 10.14.3**.  It *should* also work on other **Unix** systems and *might* work on **Windows**.

**input**

```bash
mkdir -p ~/moar-workspace
cd ~/moar-workspace
git clone git@github.com:moar-stuff/moar-sugar-example.git
git remote add fork git@github.com:${GIT_HUB_USER}/moar-sugar-example.git
git checkout -b learn origin/develop
git push fork --set-upstream 
``` 

## moar-status

The `moar-status` command shows for `~/moar-workspace`.

**input**

```bash
cd ~/moar-workspace/moar-sugar-example
moar-status
```

**output**

```console
moar-sugar-example ------------------------> fork/learn
```

If we make a change in the `moar-sugar-example` module it will show in the status.

**input**

```bash
cd ~/moar-workspace/moar-sugar-example
echo 'a' > test1.txt
echo 'b' > test2.txt
moar-status
```

**output**

```console
moar-sugar-example 2 ----------------------> fork/learn
```

The `2` indicates that two files have changed and are not yet committed.

We can commit them.

**input**

```bash
cd ~/moar-workspace/moar-sugar-example
git add -A
$git commit -m 'Two test files'
moar-status
```

**output**

```console
moar-sugar-example (1) --------------------> fork/learn
```

Now the files that chagned created a state where `moar-sugar-example` is now `(1)` commit ahead of `fork/learn`.  The `fork/learn` branch is still in sync with `origin/develop`.

We can push this change.

**input**

```bash
cd ~/moar-workspace/moar-sugar-example
git push
moar-status
```

**output**

```console
moar-sugar-example --------------------> fork/learn (1)
```

Now `fork/learn` is `(1)` commit ahead of `origin/develop`.   

The tool can also show when a branch is behind it's fork or when a fork is behind it's origin.  The following will simulate a case where the local branch is four commits behind the fork.

**input**

```bash
cd ~/moar-workspace/moar-sugar-example
git reset --hard HEAD~4
moar-status
```

**output**

```console
moar-sugar-example (0,4) --------------> fork/learn (1)
```

When a branch is behind, the display always also includes the amount ahead even if it is zero.  In this case `(0,4)` shows that our branch is `0` commits ahead and `4` commits behind `fork/learn`.

With Git, it is possible to make a change from this point.  

**input**

```bash
cd ~/moar-workspace/moar-sugar-example
echo 'c' > test3.txt
moar-status
```

**output**

```console
moar-sugar-example 1 (0,4) ------------> fork/learn (1)
```

Now the display shows `1` local change, `(0,4)` commits ahead and behind the fork.  The fork is still (1) commit ahead of `origin/master`.

We can commit our change.

**input**

```bash
cd ~/moar-workspace/moar-sugar-example
git add -A
git commit -m 'add test3'
moar-status
```

**output**

```console
moar-sugar-example (1,4) --------------> fork/learn (1)
```

From here, we could pull the changes from the fork.

**input**

```bash
git pull
moar-status
```

**output**

```console
moar-sugar-example (2) ----------------> fork/learn (1)
```

The local is now `(2)` commits ahead and `(1)` commit behind `fork/learn`.   

We can also setup a situation where the fork is behind the `origin/develop`.

**input**

```bash
git rebase HEAD~6
moar-status
```

**output**

```console
moar-sugar-example (9,8) --------------> fork/learn (1)
```

Local is now `(9,8)` with `9` ahead and `8` behind the `fork/learn` which is still `(1)` commit behind `origin/develop`  

We can force push to our fork.

**input**

```bash
git push -f
moar-status
```

**output**

```console
moar-sugar-example ------------------> fork/learn (9,7)
```

The fork is now `(9,7)` with `9` ahead and `7` behind `origin/develop`.  This makes sense because now our fork has the `2` new commits we performed but since the history is divergent the tool shows the delta.

We can rebase with `origin/develop` to get back to a state where only the two commits show.

**input**

```bash
git rebase origin/develop
moar-status
```

**output**

```console
moar-sugar-example (9,9) ------------> fork/learn (9,7)
```

**input**

```bash
git push -f
```

**output**

```console
moar-sugar-example --------------------> fork/learn (2)
``` 

The rebase created a condition where our local was `(9,9)` vs. `fork/learn`.  When we push forced we got to a state where `fork/learn` is now `2` commits ahead of `origin/develop`.

The number on status lines are also color coded with blue for local changes, green for ahead, and red for behind.  The current module is highlighted and `fork/develop` is highlighed when it differs from `origin/develop`
 
## moar-init
 
The `moar-sugar-example` module works with a library module called `moar-sugar`.  Using **Moar CLI** it's easy to link with the associated module at the correct version.

**input**

```bash
cd ~/moar-workspace/moar-sugar-example
moar-init
```

After the clone is complete, inspect the `moar-sugar` symbolic link.

Since the real location of `moar-sugar` is `~/moar-workspace` it also now shows in the `moar-status` report.

**input**

```bash
cd ~/moar-workspace/moar-sugar-example
moar-status
```

**output**

```console
moar-sugar (0,4) -----------------------> origin/master
moar-sugar-example --------------------> fork/learn (2)
```

The `moar-status` view makes it easy to see the state of the modules.  in this case, the copy of `moar-sugar` pulled into our working tree reflects the version the tree specified in `moar-modules/init-moar-sugar`.  Since the current origin/master is ahead of the state it was when the module was initialized we are showing `(0,4)`.  *(Your number is likley `(0,x)` because `moar-sugar` is in development and will move forward*.

We will keep `moar-sugar` at the initialized verison for now because we know that it is fully compatible with the designated version.

## moar-init-clone
 
We will now create a new project to learn `moar-init-clone`.

**input**

```bash
cd ~/moar-workspace
mkdir learn-moar-sugar
$cd learn-moar-sugar
git init
moar-status
```

**output**

```console
learn-moar-sugar ------------------------------------> 
moar-sugar (0,4) -----------------------> origin/master
moar-sugar-example --------------------> fork/learn (2)
```

In `learn-moar-sugar` we will add a reference to `moar-sugar`.

**input**

```bash
cd ~/moar-workspace/learn-moar-sugar
moar-init-clone git@github.com:moar-stuff/moar-sugar.git
moar-status
```

**output**

```console
learn-moar-sugar 2 ----------------------------------> 
moar-sugar (0,4) -----------------------> origin/master
moar-sugar-example --------------------> fork/learn (2)
```

After the `moar-init-clone` our status shows `2` uncommited files for `learn-moar-sugar`.  We can commit the changes.  Note, we performed this `moar-init-clone` in the context of a `moar-sugar` that was pointing at the version used by `moar-sugar-example` and as such our `learn-moar-sugar` is also pointing at that specific version.

**input**

```bash
cd ~/moar-workspace/learn-moar-sugar
git add -A
git commit -m'moar-init-clone moar-sugar'
```

The `moar-init-clone` actually created three files.  The '.gitignore' file includes a new line to ignore a `moar-sugar` symbolic link.  The `init-moar-sugar` file contains a commit hash that associates this commit with the specific commit in `moar-sugar`.  To see the exact commit simply `cat moar-modules/init-moar-sugar` to view the exact commit hash. 

**input**

```bash
cd ~/moar-workspace/learn-moar-sugar
ls
```

The following bit of bash simulates conditions where `learn-moar-sugar` exists at a proper `origin` and we have a proper fork.

**input**

```bash
cd ~/moar-workspace
mkdir -p /tmp/origin
mkdir -p /tmp/fork
git clean -fdx
git branch develop
mv learn-moar-sugar /tmp/origin/learn-moar-sugar
cp -r /tmp/origin/learn-moar-sugar /tmp/fork
```

Since `learn-moar-sugar` is now a fresh clone from it's new origin, the symbolic does not yet exist in this directory.

## moar-init

The `moar-init` command initalize the symbolic links and when needed performs clones of the referenced repositories.

Since the content in `~/moar-workspace` at this point is simply scratch work we can delete everything before exploring `moar-init`.

**input**

```bash
cd ~
rm -rf ~/moar-workspace/*
```

Now we can clone the `learn-moar-sugar` module into our clean workspace.

**input**

```bash
cd ~/moar-workspace
git clone /tmp/origin/learn-moar-sugar
cd learn-moar-sugar
git remote add fork /tmp/fork/learn-moar-sugar
git remote update
git checkout -b develop fork/develop 
ls -a
```

We don't have our symbolic until we initalize via `moar-init`.  This step setups up the symbolic and in the case of moar-sugar it also automatically adds `gradle` and `gradlew` to our project.  Since we can always get these via `moar-init` it makes sense to add them to `.gitignore`.

**input**

```bash
cd ~/moar-workspace
moar-init
echo '/gradle' > .gitignore
echo '/gradlew' > .gitignore
echo "/.gradle" >> .gitignore
echo ".classpath" >> .gitignore
echo ".project" >> .gitignore
echo ".settings" >> .gitignore
git commit -am 'ignore automatic gradle wrapper (via moar-init)'
```

**input**

```bash
moar-status
```

**output**

```console
learn-moar-sugar ------------------------> fork/develop
moar-sugar (0,4) -----------------------> origin/master
```

To speed things along let's grab some content from `moar-sugar-example`

**input**

```bash
cd ~/moar-workspace
git clone git@github.com:moar-stuff/moar-sugar-example.git
cd moar-sugar-example
moar-init
cp LICENSE build.gradle settings.gradle ../learn-moar-sugar
mkdir -p ../learn-moar-sugar/moar-sugar-app/src/main/java/moar/sugar/example
cd ../learn-moar-sugar/moar-sugar-app/src/main/java/moar/sugar/example
cp ~/moar-workspace/moar-sugar-example/moar-sugar-app/src/main/java/moar/sugar/example/BaseExample.java .
cp ~/moar-workspace/moar-sugar-example/moar-sugar-app/src/main/java/moar/sugar/example/AsyncExample.java .
cd ~/moar-workspace/learn-moar-sugar
cp ../moar-sugar-example/moar-sugar-app/build.gradle moar-sugar-app
```

**new file**

Save the following into `~/moar-workspace/learn-moar-sugar/moar-sugar-app/src/main/java/moar/sugar/example/MoarSugarExampleApp.java`.

```java
package moar.sugar.example;
  
public class MoarSugarExampleApp {
  public static void main(String[] args) {
    var out = System.out;
    (new AsyncExample(out)).run();
  }
}
```

We can run the example and then check our status.

**input**

```bash
cd ~/moar-workspace/learn-moar-sugar
./gradlew run
git commit -m 'example code'
moar-status
```

**output**

```console
learn-moar-sugar (1) --------------------> fork/develop
moar-sugar (0,4) -----------------------> origin/master
moar-sugar-example ---------------------> origin/master
``` 

We are still pointing at a old version of `moar-sugar` so let's pull the latest copy and see if works.

**input**

```bash
cd ~/moar-workspace/moar-sugar
git pull
cd ../learn-moar-sugar
./gradlew build run
moar-status
```

If the code works we can now commit the `init-moar-sugar` pointer file.

**input**

```bash
git commit -am 'updated init-moar-sugar'
moar-status
```

**output**

```console
learn-moar-sugar (2) --------------------> fork/develop
moar-sugar -----------------------------> origin/master
moar-sugar-example ---------------------> origin/master
``` 

It's easy to push this change and have confidence that others using **Moar CLI** will have accurate builds because the module references are managed with exact versions.
