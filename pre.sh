output=$(git status | grep "up to date")
if [ -z "${output}" ];
then
  git pull https://github.com/glandon22/AutoOldSchool.git
else
  echo "$output"
fi