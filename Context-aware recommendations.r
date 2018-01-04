#Context-aware recommendations using R
#laod data
raw_data = read.csv("u.data",sep="\t",header=F)
colnames(raw_data) = c("UserId","MovieId","Rating","TimeStamp")
head(raw_data)

movies = read.csv("u.item",sep="|",header=F)
colnames(movies) = c("MovieId","MovieTitle","ReleaseDate","VideoReleaseDate","IMDbURL","Unknown",
                     "Action","Adventure","Animation","Children","Comedy","Crime","Documentary",
                     "Drama","Fantasy","FilmNoir","Horror","Musical","Mystery","Romance","SciFi",
                     "Thriller","War","Western")

#remove unwanted columns from the data frame
movies = movies[,-c(2:5)]

#merge the Movies and Ratings datasets
ratings_ctx = merge(x = raw_data, y = movies, by = "MovieId", all.x = TRUE)

# the context is the hour of the day

# create context profile

ts = ratings_ctx$TimeStamp

# convert it into a POSIXlt date object and using hour property to extract hour of the day:
hours <- as.POSIXlt(ts,origin="1960-10-01")$hour

#can append the hours back on to the ratings dataset
ratings_ctx = data.frame(cbind(ratings_ctx,hours))

# building a context profile for a user with the user ID 943:

# Extract ratings information for the active user(943)
# removing UserId, MovieId, Rating,Timestamp columns

UCP = ratings_ctx[(ratings_ctx$UserId == 943),][,-c(2,3,4,5)]

# compute the columns of all the item features. This columnwise sum is used
# to compute the preferences for the item features for each hour of the day

UCP_pref = aggregate(.~hours,UCP[,-1],sum)

#normalize the preceding data between 0-1
UCP_pref_sc = cbind(context = UCP_pref[,1],t(apply(UCP_pref[,-1], 1,
                                                   function(x)(x-min(x))/(max(x)-min(x)))))


# Generating context-aware recommendations
#reuse the recommend object built using R, which contains content recommendations for all the users

recommend$MovieId

# add our time or hour of the day dimension and then generate recommendations as per the current context

# merge recommendations and movies dataset
UCP_pref_content = merge(x = recommend, y = movies, by = "MovieId", all.x = TRUE)

# required matrices, user context profile (UCP_Pref_SC) and user content recommendations (UCP_Pref_content)
# now computed

# generate recommendations for the user at the ninth hour

# Performing element wise multiplication for the User content recommendations and the ninth
# hour context preferences for the user

active_user =cbind(UCP_pref_content$MovieId,
                   (as.matrix(UCP_pref_content[,-c(1,2,3)]) %*% as.matrix(UCP_pref_sc[4,2:19])))

# create a dataframe object of the prediction object
active_user_df = as.data.frame(active_user)

#add column names to the predictions object
names(active_user_df) = c('MovieId','SimVal')

# sort the results
FinalPredicitons_943 = active_user_df[order(-active_user_df$SimVal),]




















names(movies)
str(movies)

users = read.csv("u.user",sep="|",header=F)
colnames(users) = c("UserId","Age","Gender","Occupation","ZipCode")
users = users[,-c(4,5)]

#basic stats of data
dim(movies)
dim(users)


#merging user item rating with user profile
ratings = merge(x = ratings, y = movies, by = "MovieId", all.x = TRUE)

names(ratings)

#remove unwanted columns
#ratings = ratings[,-c(4,8:12)]

#conversion to categorical
nrat = unlist(lapply(ratings$Rating,function(x)
{
  if(x>3) {return(1)}
  else {return(0)}
}))
ratings = cbind(ratings,nrat)

apply(ratings[,-c(1:3,23)],2,function(x)table(x))

scaled_ratings = ratings[,-c(3,4)]

scaled_ratings=scale(scaled_ratings[,-c(1,2,21)])

scaled_ratings = cbind(scaled_ratings,ratings[,c(1,2,23)])

head(scaled_ratings)

# create a randomize index object of all the data. Then we use this
# indexes to divide the train and test sets
set.seed(7)
which_train <- sample(x = c(TRUE, FALSE), size = nrow(scaled_ratings),replace = TRUE, prob = c(0.8, 0.2))
model_data_train <- scaled_ratings[which_train, ]
model_data_test <- scaled_ratings[!which_train, ]

dim(model_data_train)
dim(model_data_test)

# converting the integer nrat variable to factor format
library(randomForest)
fit = randomForest(as.factor(nrat)~., data = model_data_train[,-c(19,20)])

fit

summary(fit)

predictions <- predict(fit, model_data_test[,-c(19,20,21)], type="class")

#building confusion matrix
cm = table(predictions,model_data_test$nrat)
(accuracy <- sum(diag(cm)) / sum(cm))
(precision <- diag(cm) / rowSums(cm))
recall <- (diag(cm) / colSums(cm))

recall

# generate the top-N recommendations to a user ID (943)

#1. Create a DataFrame containing all the movies not rated by the active user (943).

#extract distinct movieids
totalMovieIds = unique(movies$MovieId)

#a function to generate dataframe which creates non-rated
# movies by active user and set rating to 0;
nonratedmoviedf = function(userid){
  ratedmovies = raw_data[raw_data$UserId==userid,]$MovieId
  non_ratedmovies = totalMovieIds[!totalMovieIds %in%
                                    ratedmovies]
  df = data.frame(cbind(rep(userid),non_ratedmovies,0))
  names(df) = c("UserId","MovieId","Rating")
  return(df)
}
#extracting non-rated movies for active userid 943
activeusernonratedmoviedf = nonratedmoviedf(943)

# 2. Build a profile for this active user via DataFrame

activeuserratings = merge(x = activeusernonratedmoviedf,
                          y = movies, by = "MovieId", all.x = TRUE)

# 3. Predict ratings, sort and generate 10 recommendations.

predictions <- predict(fit, activeuserratings[,-c(1:4)],type="class")
#creating a dataframe from the results
recommend = data.frame(movieId = activeuserratings$MovieId,predictions)
# remove all the movies which the model has predicted as 0 and
# then use the remaining items as more probable movies
# which might be liked by the active user.
recommend = recommend[which(recommend$predictions == 1),]

dim(recommend)